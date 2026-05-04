package org.accompany.backendchatbot.domain.chat.service;

import org.accompany.backendchatbot.domain.chat.dto.request.AiChatReq;
import org.accompany.backendchatbot.domain.chat.dto.response.ChatRes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {
    ChatRes chat(AiChatReq request);
    SseEmitter streamChat(AiChatReq request);
}
