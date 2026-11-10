package com.sushil.base_project.core.security;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.sushil.base_project.core.audit.AuditLogService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuditLogService auditLog;

    public CustomAccessDeniedHandler(AuditLogService auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public void handle(HttpServletRequest request, 
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Object principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication() == null
                        ? null
                        : org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication().getPrincipal();
        String subject = principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails
                ? userDetails.getUsername()
                : null;
        auditLog.record(
                "ACCESS_DENIED", subject, request.getRemoteAddr(),
                request.getHeader("User-Agent"), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("status", HttpServletResponse.SC_FORBIDDEN);
        errorDetails.put("error", "Forbidden");
        errorDetails.put("message", "You do not have permission to access this resource");
        errorDetails.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), errorDetails);
    }
}