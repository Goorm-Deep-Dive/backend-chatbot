package org.accompany.backendchatbot.domain.chat.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AiChatReq(
        Long userId,
        @NotBlank(message = "메시지는 필수 입니다.")
        String message,

        @Valid
        AiChatContext context,

        List<AiChatMessage> history
) {
}
