package org.accompany.backendchatbot.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accompany.backendchatbot.domain.chat.client.AiClient;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChatReq;
import org.accompany.backendchatbot.domain.chat.dto.response.ChatRes;
import org.accompany.backendchatbot.domain.chat.prompt.ChatPromptBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final AiClient aiClient;
    private final ChatPromptBuilder chatPromptBuilder;

    @Override
    public ChatRes chat(AiChatReq request) {
        log.info("[Chat Server] 질문 요청 - userId={}",
                request.userId()
        );

        String systemPrompt = chatPromptBuilder.buildSystemPrompt(request);
        String userPrompt = chatPromptBuilder.buildUserPrompt(request);
        String answer = aiClient.ask(systemPrompt, userPrompt);

        if (answer == null || answer.isBlank()) {
            log.warn("[Chat Server] AI 응답이 비어 있습니다. userId={}", request.userId());
            throw new IllegalStateException("AI 응답이 비어 있습니다.");
        }

        log.info("[Chat] 답변 생성 완료 - userId={}", request.userId());

        return new ChatRes(answer);
    }

}
