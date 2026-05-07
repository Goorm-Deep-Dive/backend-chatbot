package org.accompany.backendchatbot.domain.chat.dto.request;

import java.util.List;

public record AiChecklistSummary(
        List<String> notCompleted,
        List<String> urgent,
        List<String> completed,
        int notCompletedTotalCount
) {
}