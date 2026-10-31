package com.sushil.base_project.auth.models;

import jakarta.servlet.http.HttpServletRequest;

public record ClientInfo(String userAgent, String ip) {

    private static final int MAX_UA_LENGTH = 512;

    public static ClientInfo from(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if( ua != null && ua.length() > MAX_UA_LENGTH) {
            ua = ua.substring(0,MAX_UA_LENGTH);
        }

        return new ClientInfo(ua, request.getRemoteAddr());
    }
}
