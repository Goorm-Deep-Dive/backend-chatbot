package org.accompany.backendchatbot.domain.chat.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiClient implements AiClient {

    private final ChatClient chatClient;

    @Override
    public String ask(String systemPrompt, String userMessage) {
        try {
            log.info("[OpenAI] AI 호출 시작");

            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            log.info("[OpenAI] AI 호출 완료");

            return content;
        } catch (RuntimeException e) {
            log.error("[OpenAI] AI 호출 실패", e);
            throw e;
        }
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userMessage) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content();
    }
}
