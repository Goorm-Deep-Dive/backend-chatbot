package org.accompany.backendchatbot.domain.chat.client;

import reactor.core.publisher.Flux;

public interface AiClient {
    String ask(String systemPrompt, String userMessage);
    Flux<String> stream(String systemPrompt, String userMessage);
}
