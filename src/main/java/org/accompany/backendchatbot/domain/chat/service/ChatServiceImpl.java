package org.accompany.backendchatbot.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accompany.backendchatbot.domain.chat.client.AiClient;
import org.accompany.backendchatbot.domain.chat.dto.request.AiChatReq;
import org.accompany.backendchatbot.domain.chat.dto.response.ChatRes;
import org.accompany.backendchatbot.domain.chat.prompt.ChatPromptBuilder;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final AiClient aiClient;
    private final ChatPromptBuilder chatPromptBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public ChatRes chat(AiChatReq request) {
        log.info("[Chat Server] 질문 요청 - userId={}",
                request.userId()
        );

        String systemPrompt = chatPromptBuilder.buildSystemPrompt();
        String userPrompt = chatPromptBuilder.buildUserPrompt(request);
        String answer = aiClient.ask(systemPrompt, userPrompt);

        if (answer == null || answer.isBlank()) {
            log.warn("[Chat Server] AI 응답이 비어 있습니다. userId={}", request.userId());
            throw new IllegalStateException("AI 응답이 비어 있습니다.");
        }

        log.info("[Chat] 답변 생성 완료 - userId={}", request.userId());

        return new ChatRes(answer);
    }

    @Override
    public SseEmitter streamChat(AiChatReq request) {
        SseEmitter emitter = new SseEmitter(120_000L);

        log.info("[Chat:SSE] 스트리밍 요청 시작 - userId={}",
                request.userId());
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        Thread.ofVirtual().start(() -> {
            if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }

            AtomicInteger tokenCount = new AtomicInteger();

            try {
                String systemPrompt = chatPromptBuilder.buildSystemPrompt();
                String userPrompt = chatPromptBuilder.buildUserPrompt(request);

                for (String token : aiClient.stream(systemPrompt, userPrompt).toIterable()) {
                    tokenCount.incrementAndGet();
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(token)));
                }

                log.info("[Chat:SSE] 스트리밍 정상 완료 - tokenCount={}",
                        tokenCount.get());

                emitter.complete();

            } catch (IOException e) {
                log.warn("[Chat:SSE] 클라이언트 연결 종료 - tokenCount={}",
                        tokenCount.get());
                emitter.complete();

            } catch (Exception e) {
                log.error("[Chat:SSE] 스트리밍 실패 - tokenCount={}, error={}",
                        tokenCount.get(), e.getMessage(), e);
                emitter.completeWithError(e);
            } finally {
                MDC.clear();
            }
        });

        emitter.onTimeout(() -> {
            log.warn("[Chat:SSE] 타임아웃 발생 - userId={}",
                    request.userId());

            emitter.complete();
        });

        return emitter;
    }

}
