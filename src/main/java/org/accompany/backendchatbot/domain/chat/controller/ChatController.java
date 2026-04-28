package org.accompany.backendchatbot.domain.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChatReq;
import org.accompany.backendchatbot.domain.chat.dto.response.ChatRes;
import org.accompany.backendchatbot.domain.chat.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    public ChatRes chat(@Valid @RequestBody AiChatReq request) {
        return chatService.chat(request);
    }
}
