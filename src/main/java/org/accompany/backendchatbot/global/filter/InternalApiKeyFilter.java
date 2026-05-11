package org.accompany.backendchatbot.global.filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/actuator/health")
                || request.getDispatcherType() == DispatcherType.ASYNC
                || request.getDispatcherType() == DispatcherType.ERROR;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String apiKey = request.getHeader("X-Internal-API-Key");

        if (apiKey == null || apiKey.isBlank()) {
            log.error("[InternalApiKeyFilter] API Key 없음 - path={}, method={}, ip={}",
                    request.getRequestURI(),
                    request.getMethod(),
                    request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8))) {
            log.error("[InternalApiKeyFilter] API Key 불일치 - path={}, method={}, ip={}",
                    request.getRequestURI(),
                    request.getMethod(),
                    request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
                new PreAuthenticatedAuthenticationToken("internal", null, List.of())
        );

        filterChain.doFilter(request, response);
    }
}
