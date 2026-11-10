package com.sushil.base_project.core.security;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sushil.base_project.core.audit.AuditLogService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final AuditLogService auditLog;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${app.security.auth-rate-limit.requests:10}")
    private int maxRequests;

    @Value("${app.security.auth-rate-limit.window:1m}")
    private Duration windowDuration;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        Window current = windows.get(clientIp);
        if (current == null && windows.size() >= MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt() >= windowDuration.toMillis());
            if (windows.size() >= MAX_TRACKED_CLIENTS) {
                reject(request, response, clientIp);
                return;
            }
        }

        Window window = windows.compute(clientIp, (ip, previous) ->
                previous == null || now - previous.startedAt() >= windowDuration.toMillis()
                        ? new Window(now, 1)
                        : new Window(previous.startedAt(), previous.requests() + 1));

        if (window.requests() > maxRequests) {
            reject(request, response, clientIp);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String clientIp)
            throws IOException {
        auditLog.record(
                "ACCESS_DENIED", clientIp, clientIp, request.getHeader("User-Agent"), "auth_rate_limit");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"Too many authentication requests\"}");
    }

    private record Window(long startedAt, int requests) {
    }
}
