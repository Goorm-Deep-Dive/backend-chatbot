package org.accompany.backendchatbot.domain.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final int MIN_CHUNK_LENGTH = 80;

    private final VectorStore vectorStore;

    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(500)
            .withMinChunkSizeChars(200)
            .withMinChunkLengthToEmbed(50)
            .withKeepSeparator(true)
            .build();

    @Override
    public void ingestDocuments(List<MultipartFile> files, List<String> titles) {
        for (int i = 0; i < files.size(); i++) {
            String title = (titles != null && i < titles.size()) ? titles.get(i) : null;
            ingestDocument(files.get(i), title);
        }
    }

    /**
     * 파일을 파싱하고, 텍스트 정제 -> 청킹 -> 짧은 청크 병합 -> 유효성 검사 후 벡터 저장소에 저장
     */
    @Override
    public void ingestDocument(MultipartFile file, String title) {
        String filename = file.getOriginalFilename();
        String displayTitle = (title != null && !title.isBlank()) ? title : stripExtension(filename);
        log.info("[Document] 문서 적재 시작 - filename={}", filename);

        List<Document> documents = parseDocument(file).stream()
                .map(doc -> {
                    doc.getMetadata().put("source_title", displayTitle);
                    doc.getMetadata().put("filename", filename);
                    doc.getMetadata().put("source_type", "uploaded_file");
                    doc.getMetadata().put("content_type", file.getContentType());
                    doc.getMetadata().put("uploaded_at", LocalDateTime.now().toString());

                    return new Document(
                            cleanText(doc.getText()),
                            doc.getMetadata()
                    );
                })
                .toList();

        // 1. 문서를 일정 크기로 청킹
        List<Document> splitChunks = splitter.split(documents);

        // 2. 너무 짧은 청크는 버리지 않고 이전 청크에 병합
        List<Document> mergedChunks = mergeShortChunks(splitChunks);

        // 3. 특수문자 비율이 높은 쓰레기 청크만 제거하고, 출처 문구 추가
        List<Document> chunks = mergedChunks.stream()
                .filter(doc -> isValidChunk(doc.getText()))
                .map(doc -> {
                    String sourceTitle = (String) doc.getMetadata().getOrDefault("source_title", "");
                    return new Document(
                            "[문서: " + sourceTitle + "]\n" + doc.getText(),
                            doc.getMetadata()
                    );
                })
                .toList();

        if (chunks.isEmpty()) {
            log.warn("[Document] 저장 가능한 청크 없음 - filename={}", filename);
            return;
        }

        vectorStore.add(chunks);

        log.info("[Document] 문서 적재 완료 - filename={}, chunks={}", filename, chunks.size());
    }

    /**
     * Tika를 사용해 업로드 파일을 Document로 변환
     */
    private List<Document> parseDocument(MultipartFile file) {
        try {
            Resource resource = new InputStreamResource(file.getInputStream()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            return new TikaDocumentReader(resource).read();
        } catch (Exception e) {
            log.error("[Document] 문서 파싱 실패 - filename={}", file.getOriginalFilename(), e);
            throw new IllegalArgumentException("문서 파싱에 실패했습니다.", e);
        }
    }

    /**
     * Tika가 추출한 원문에서 HTML/CSS, 깨진 문자열, 불필요한 특수문자 반복 등을 정리
     */
    private String cleanText(String text) {
        if (text == null) return "";

        return text
                // 법령 조문 링크 query parameter 단독 라인 제거
                .replaceAll("(?m)^.*docType=JO.*$", " ")

                // 법령 조문 링크 제거
                .replaceAll("https?://www\\.law\\.go\\.kr/LSW/LsiJoLinkP\\.do\\?\\S+", " ")

                // URL 인코딩이 길게 깨진 문자열 제거
                .replaceAll("(%[0-9A-Fa-f]{2}){2,}", " ")

                // HTML 태그 제거
                .replaceAll("(?i)<[^>]+>", " ")

                // CSS/HTML 속성 형태 제거
                .replaceAll("(?i)(style|class|font|width|height|margin|padding|border)\\s*=\\s*[^\\s]+", " ")

                // 긴 특수문자 반복 제거
                .replaceAll("[=\\-_/\\\\*#]{5,}", " ")

                // 제어문자 제거, 줄바꿈은 유지
                .replaceAll("[\\u0000-\\u0009\\u000B\\u000C\\u000E-\\u001F\\u007F]", "")

                .replaceAll("(?m)^\\s+$", "")
                // 공백 정리
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")


                .trim();
    }

    /**
     * 너무 짧은 청크는 버리지 않고 이전 청크에 붙임
     */
    private List<Document> mergeShortChunks(List<Document> chunks) {
        List<Document> merged = new ArrayList<>();

        for (Document chunk : chunks) {
            String text = chunk.getText();

            if (text == null || text.isBlank()) {
                continue;
            }

            if (merged.isEmpty()) {
                merged.add(chunk);
                continue;
            }

            if (text.length() < MIN_CHUNK_LENGTH) {
                Document prev = merged.remove(merged.size() - 1);

                merged.add(new Document(
                        prev.getText() + "\n" + text,
                        prev.getMetadata()
                ));
            } else {
                merged.add(chunk);
            }
        }

        return merged;
    }

    /**
     * 벡터 DB에 저장할 만한 정상 청크인지 검사
     */
    private boolean isValidChunk(String text) {
        if (text == null || text.isBlank()) return false;

        long letterOrDigitCount = text.chars()
                .filter(Character::isLetterOrDigit)
                .count();

        long specialCount = text.chars()
                .filter(c -> !Character.isLetterOrDigit(c)
                        && !Character.isWhitespace(c))
                .count();

        double specialRatio = (double) specialCount / text.length();

        // 의미 있는 글자/숫자가 너무 적으면 제거
        if (letterOrDigitCount < 40) return false;

        // 특수문자 비율이 너무 높으면 제거
        if (specialRatio > 0.4) return false;

        return true;
    }

    private String stripExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}