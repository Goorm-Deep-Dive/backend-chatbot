package org.accompany.backendchatbot.domain.chat.prompt;

import org.accompany.backendchatbot.domain.chat.dto.request.AiChatContext;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChatMessage;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChatReq;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChecklistSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatPromptBuilder {

    private static final int MAX_HISTORY_SIZE = 8;

    /**
     * System Prompt - 짧고 명확하게 유지
     */
    public String buildSystemPrompt(AiChatReq request) {
        return """
            당신은 '동행' 서비스의 AI 상담사입니다.
            사망 이후 행정 절차, 금융·보험 정리, 상속 준비를 한국어로 안내합니다.
        
            규칙:
            - 질문에 먼저 답합니다.
            - 제공된 정보만 사용하고 추측하지 않습니다.
            - 법률·세무 판단은 참고 정보로만 설명합니다.
            - 필요 시 기관, 세무사, 법무사, 변호사 확인을 안내합니다.
            - 절차나 준비 사항을 안내한 경우, 답변 마지막에 관련 기관 문의 또는 동행 서비스의 체크리스트 확인을 함께 안내합니다.
            - 답변은 핵심 위주로 짧게 작성합니다.
            """;
    }

    /**
     * User Prompt
     * - context + history + question 구조
     * - 이후 RAG 문서 블록 추가 가능하도록 설계
     */
    public String buildUserPrompt(AiChatReq request) {
        StringBuilder prompt = new StringBuilder();

        appendContext(prompt, request.context());
        appendHistory(prompt, request.history());

        prompt.append("\n[질문]\n")
                .append(safeText(request.message()));

        return prompt.toString();
    }
    /**
     * 사용자 상황 정보
     */
    private void appendContext(StringBuilder prompt, AiChatContext context) {
        if (context == null) {
            return;
        }

        prompt.append("[상황]\n");

        appendIfPresent(prompt, "사용자", context.userName());

        AiChecklistSummary summary = context.checklistSummary();
        if (summary != null) {
            appendList(prompt, "미완료", summary.notCompleted());
            appendList(prompt, "임박", summary.urgent());
            appendList(prompt, "완료", summary.completed());
        }
    }

    /**
     * 최근 대화 히스토리
     */
    private void appendHistory(StringBuilder prompt, List<AiChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return;
        }

        prompt.append("\n[최근대화]\n");

        history.stream()
                .skip(Math.max(0, history.size() - MAX_HISTORY_SIZE))
                .forEach(message -> prompt
                        .append(normalizeRole(message.role()))
                        .append(": ")
                        .append(safeText(message.content()))
                        .append("\n"));
    }

    /**
     * 값이 존재할 경우만 출력
     */
    private void appendIfPresent(StringBuilder prompt, String label, Object value) {
        if (value != null && !value.toString().isBlank()) {
            prompt.append("- ")
                    .append(label)
                    .append(": ")
                    .append(value)
                    .append("\n");
        }
    }

    private void appendList(StringBuilder prompt, String label, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        prompt.append("- ")
                .append(label)
                .append(": ")
                .append(String.join(", ", values))
                .append("\n");
    }

    /**
     * 역할 정규화
     */
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "알 수 없음";
        }

        return switch (role.toUpperCase()) {
            case "USER" -> "사용자";
            case "AI", "ASSISTANT" -> "챗봇";
            default -> role;
        };
    }

    /**
     * null-safe 문자열 처리
     */
    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}