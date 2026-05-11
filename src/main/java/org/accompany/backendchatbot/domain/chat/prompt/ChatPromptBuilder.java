package org.accompany.backendchatbot.domain.chat.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChatContext;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChatMessage;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChatReq;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChecklistSummary;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPromptBuilder {

    private static final int MAX_HISTORY_SIZE = 8;
    private final VectorStore vectorStore;

    /**
     * System Prompt - 짧고 명확하게 유지
     */
    public String buildSystemPrompt() {
        return """
        당신은 '동행' 서비스의 AI 상담사입니다.
        사망 이후 행정 절차, 법원행정, 금융·보험 정리, 상속 준비, 디지털 분야를 한국어로 안내합니다.
        위 범위를 벗어난 질문에는 답변하지 않고 서비스 범위를 안내합니다.
    
        규칙:
        [답변 태도]
        - 차분하고 간결하게 답변하며, 핵심 위주로 작성합니다.
        - 절차와 단계는 번호 목록 또는 문단으로 자연스럽게 구분합니다.
        - 중요한 기한은 별도 문장으로 강조합니다.
        [참고문서]
        - [참고문서]가 있으면 해당 내용만 사용하며, 자체 지식과 혼용하지 않습니다.
        - [참고문서]가 없으면 핵심 절차만 간략히 안내하고, 자세한 내용은 관련 기관 또는 전문가 확인을 권장합니다.
        - [참고문서]에 법적 근거가 명시된 경우, 답변 마지막에 핵심 조항만 간략하게 표기합니다.
        - 법률·세무 판단은 참고 정보 수준으로만 설명하며, 필요 시 세무사·법무사·변호사 확인을 안내합니다.
        [체크리스트]
        - [사용자 체크리스트 현황]에 있는 절차만 진행 상태를 언급하고, 목록에 없는 절차의 진행 여부는 언급하지 않습니다.
        - [사용자 체크리스트 현황]에 없는 절차를 임의로 추가하거나 완료 여부를 추측하지 않습니다.
        - [사용자 체크리스트 현황] 정보가 없으면 일반적으로 먼저 확인할 절차만 간단히 안내합니다.
        - 임박한 절차가 있으면 가장 먼저 안내합니다.
        - 질문과 관련된 미완료 절차가 있을 경우에만 동행 서비스의 체크리스트 확인을 자연스럽게 안내합니다.
        """;
    }

    /**
     * User Prompt
     * - context + history + question + rag
     */
    public String buildUserPrompt(AiChatReq request) {
        StringBuilder prompt = new StringBuilder();

        appendContext(prompt, request.context());
        appendHistory(prompt, request.history());
        appendRelevantDocs(prompt, request.message());

        prompt.append("\n[질문]\n")
                .append(safeText(request.message()));

        return prompt.toString();
    }
    /**
     * 사용자 상황 정보
     */
    private void appendContext(StringBuilder prompt, AiChatContext context) {
        if (context == null) {
            return;
        }

        AiChecklistSummary summary = context.checklistSummary();

        boolean hasSummary = summary != null &&
                (summary.notCompletedWithDeadline() != null && !summary.notCompletedWithDeadline().isEmpty() ||
                        summary.notCompletedUrgent() != null && !summary.notCompletedUrgent().isEmpty() ||
                        summary.completed() != null && !summary.completed().isEmpty());

        if (context.dateOfDeath() == null && !hasSummary) return;

        prompt.append("[사용자 체크리스트 현황]\n");
        prompt.append("- 오늘 날짜: ").append(LocalDate.now()).append("\n");
        appendIfPresent(prompt, "고인 사망일", context.dateOfDeath());

        if (summary != null) {
            appendDueItems(prompt, summary.notCompletedWithDeadline());
            appendList(prompt, "즉시 처리 필요 (마감일 없음)", summary.notCompletedUrgent());
            appendList(prompt, "미완료 (마감일 없음)", summary.notCompletedNoDueDate());
            appendList(prompt, "완료", summary.completed());
        }
    }

    /**
     * 최근 대화 히스토리
     */
    private void appendHistory(StringBuilder prompt, List<AiChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return;
        }

        prompt.append("\n[최근대화]\n");

        history.stream()
                .skip(Math.max(0, history.size() - MAX_HISTORY_SIZE))
                .forEach(message -> prompt
                        .append(normalizeRole(message.role()))
                        .append(": ")
                        .append(safeText(message.content()))
                        .append("\n"));
    }

    /**
     * 참고 문서
     */
    private void appendRelevantDocs(StringBuilder prompt, String message) {

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(message)
                        .topK(3) // 유사한 문서 개수
                        .similarityThreshold(0.5) // 유사도
                        .build()
        );

        log.info("[RAG] 검색 결과 - query={}, docs={}", message, docs.size());

        if(docs.isEmpty()) {
            return;
        }

        prompt.append("\n[참고문서]\n");
        docs.forEach(doc -> {
            prompt.append(doc.getText())
                    .append("\n---\n");
        });
    }

    /**
     * 값이 존재할 경우만 출력
     */
    private void appendIfPresent(StringBuilder prompt, String label, Object value) {
        if (value != null && !value.toString().isBlank()) {
            prompt.append("- ")
                    .append(label)
                    .append(": ")
                    .append(value)
                    .append("\n");
        }
    }

    private void appendList(StringBuilder prompt, String label, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        prompt.append("- ")
                .append(label)
                .append(": ")
                .append(String.join(", ", values))
                .append("\n");
    }

    private void appendDueItems(StringBuilder prompt, List<AiChecklistSummary.DueItem> items) {
        if (items == null || items.isEmpty()) return;

        prompt.append("- 마감일 있는 미완료: ");
        items.forEach(item ->
                prompt.append(item.name())
                        .append(" (")
                        .append(item.dueDate())
                        .append("까지), ")
        );
        prompt.deleteCharAt(prompt.length() - 2); // 마지막 쉼표 제거
        prompt.append("\n");
    }

    /**
     * 역할 정규화
     */
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "알 수 없음";
        }

        return switch (role.toUpperCase()) {
            case "USER" -> "사용자";
            case "AI", "ASSISTANT" -> "챗봇";
            default -> role;
        };
    }

    /**
     * null-safe 문자열 처리
     */
    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}