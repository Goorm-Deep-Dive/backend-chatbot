package org.accompany.backendchatbot.domain.chat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GeminiAiClient extends AbstractAiClient {

    public GeminiAiClient(@Qualifier("gemini") ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    protected String providerName() { return "Gemini"; }
}
