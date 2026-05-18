package org.accompany.backendchatbot.domain.document.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accompany.backendchatbot.domain.document.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/documents")
public class DocumentController {

    private final DocumentService documentService;

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf", // pdf
            "application/msword", // 구버전 word
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // 신버전 word
            "text/plain" // .txt
    );

    @PostMapping
    public ResponseEntity<Void> uploadDocument(@RequestParam("files") List<MultipartFile> files,
                                               @RequestPart(required = false) List<String> titles) {
        for (MultipartFile file : files) {
            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + file.getContentType());
            }
        }

        documentService.ingestDocuments(files, titles);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
