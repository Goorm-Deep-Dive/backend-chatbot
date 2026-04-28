package org.accompany.backendchatbot.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AiChatMessage(
        @NotBlank(message = "메시지 역할은 필수 입니다.")
        String role,

        @NotBlank(message = "메시지 내용은 필수 입니다.")
        String content
) {
}
