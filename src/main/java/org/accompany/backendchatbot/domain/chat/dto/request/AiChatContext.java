package org.accompany.backendchatbot.domain.chat.dto.request;

public record AiChatContext(
        String userName,
        AiChecklistSummary checklistSummary
) {
}
