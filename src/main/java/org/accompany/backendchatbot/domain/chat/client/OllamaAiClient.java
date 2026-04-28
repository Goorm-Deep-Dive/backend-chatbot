package org.accompany.backendchatbot.domain.chat.client;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
public class OllamaAiClient implements AiClient {

    private final ChatClient chatClient;

    @Override
    public String ask(String systemPrompt, String userMessage) {
        try {
            log.info("[Ollama] AI 호출 시작");

            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            log.info("[Ollama] AI 호출 완료");
            return content;
        } catch (RuntimeException e) {
            log.error("[Ollama] AI 호출 실패", e);
            throw e;
        }
    }
}
