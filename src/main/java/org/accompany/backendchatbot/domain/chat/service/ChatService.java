package org.accompany.backendchatbot.domain.chat.service;

import org.accompany.backendchatbot.domain.chat.dto.request.AiChatReq;
import org.accompany.backendchatbot.domain.chat.dto.response.ChatRes;

public interface ChatService {
    ChatRes chat(AiChatReq request);
}
