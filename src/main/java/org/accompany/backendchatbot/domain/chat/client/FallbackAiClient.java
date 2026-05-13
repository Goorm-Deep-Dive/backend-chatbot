package org.accompany.backendchatbot.domain.chat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Primary
@Component
public class FallbackAiClient implements AiClient {

    private final List<AiClient> clients;

    public FallbackAiClient(
            OpenAiClient openAiClient,
            GeminiAiClient geminiClient,
            @Value("${app.ai.providers}") List<String> providers
    ) {
        Map<String, AiClient> clientMap = Map.of(
                "openai", openAiClient,
                "gemini", geminiClient
        );

        this.clients = providers.stream()
                .map(clientMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public String ask(String systemPrompt, String userMessage) {
        if (clients.isEmpty()) {
            throw new IllegalStateException("[Fallback] 클라이언트 없음");
        }

        RuntimeException last = null;

        for(AiClient client : clients) {
            try {
                return client.ask(systemPrompt, userMessage);
            } catch (RuntimeException e) {
                log.warn("[Fallback] {} 실패 → 다음 클라이언트로 전환", client.getClass().getSimpleName(), e);
                last = e;
            }
        }
        throw last;
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userMessage) {
        if (clients.isEmpty()) {
            return Flux.error(new IllegalStateException("[Fallback] 클라이언트 없음"));
        }

        Flux<String> result = clients.getFirst().stream(systemPrompt, userMessage)
                .doOnSubscribe(s ->
                        log.info("[Fallback] {} 스트리밍 시도", clients.getFirst().getClass().getSimpleName()));

        for (int i = 1; i < clients.size(); i++) {
            final AiClient next = clients.get(i);
            result = result.onErrorResume(e -> {
                log.warn("[Fallback] 스트리밍 실패 → 다음 클라이언트로 전환", e);
                return next.stream(systemPrompt, userMessage)
                        .doOnSubscribe(s ->
                                log.info("[Fallback] {} 스트리밍 시도", next.getClass().getSimpleName()));
            });
        }

        return result;
    }
}
