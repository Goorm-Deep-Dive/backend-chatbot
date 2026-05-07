package org.accompany.backendchatbot.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 문서 파싱 실패, AI 응답 없음 등 명시적 예외
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRes> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[Exception] {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorRes(e.getMessage()));
    }

    // AI 응답 비어있음
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorRes> handleIllegalState(IllegalStateException e) {
        log.error("[Exception] {}", e.getMessage());
        return ResponseEntity.internalServerError().body(new ErrorRes(e.getMessage()));
    }

    // 그 외 예상치 못한 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRes> handleException(Exception e) {
        log.error("[Exception] 처리되지 않은 예외", e);
        return ResponseEntity.internalServerError().body(new ErrorRes("서버 오류가 발생했습니다."));
    }
}
