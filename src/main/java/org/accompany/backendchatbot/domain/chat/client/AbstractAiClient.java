package org.accompany.backendchatbot.domain.chat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

@Slf4j
public abstract class AbstractAiClient implements AiClient {

    private final ChatClient chatClient;

    protected AbstractAiClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    protected abstract String providerName();

    @Override
    public String ask(String systemPrompt, String userMessage) {
        try {
            log.info("[{}] AI 호출 시작", providerName());
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
            log.info("[{}] AI 호출 완료", providerName());
            return content;
        } catch (RuntimeException e) {
            log.error("[{}] AI 호출 실패", providerName(), e);
            throw e;
        }
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userMessage) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content()
                .doOnError(e -> log.error("[{}] 스트리밍 실패", providerName(), e));
    }
}
