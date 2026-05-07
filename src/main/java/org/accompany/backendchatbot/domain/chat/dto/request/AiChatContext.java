package org.accompany.backendchatbot.domain.chat.dto.request;

import java.time.LocalDate;

public record AiChatContext(
        LocalDate dateOfDeath,
        AiChecklistSummary checklistSummary
) {
}
