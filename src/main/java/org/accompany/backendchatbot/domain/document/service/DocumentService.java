package org.accompany.backendchatbot.domain.document.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    void ingestDocuments(List<MultipartFile> files);
    void ingestDocument(MultipartFile file);
}
