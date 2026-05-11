package org.accompany.backendchatbot.domain.chat.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
public class OllamaAiClient extends AbstractAiClient {

    public OllamaAiClient(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    protected String providerName() { return "Ollama"; }
}
