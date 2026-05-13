package org.accompany.backendchatbot.domain.chat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicLong;

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
            long start = System.currentTimeMillis();

            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
            log.info("[{}] AI 응답 완료 - {}ms", providerName(), System.currentTimeMillis() - start);
            return content;
        } catch (RuntimeException e) {
            log.error("[{}] AI 호출 실패", providerName(), e);
            throw e;
        }
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userMessage) {
        long start = System.currentTimeMillis();
        AtomicLong firstTokenTime = new AtomicLong();

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content()
                .doOnNext(token -> {
                    if (firstTokenTime.compareAndSet(0, System.currentTimeMillis())) {
                        log.info("[{}] 첫 토큰 수신 - {}ms", providerName(), firstTokenTime.get() - start);
                    }
                })
                .doOnComplete(() ->
                        log.info("[{}] 스트리밍 완료 - {}ms", providerName(), System.currentTimeMillis() - start))
                .doOnError(e -> log.error("[{}] 스트리밍 실패", providerName(), e));
    }
}
