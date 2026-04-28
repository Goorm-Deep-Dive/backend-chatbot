package org.accompany.backendchatbot.domain.chat.client;

public interface AiClient {
    String ask(String systemPrompt, String userMessage);
}
