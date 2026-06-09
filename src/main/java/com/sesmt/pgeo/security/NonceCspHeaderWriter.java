package com.sesmt.pgeo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;

public class NonceCspHeaderWriter implements HeaderWriter {

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        String nonce = (String) request.getAttribute(CspNonceFilter.NONCE_ATTR);
        if (nonce == null) return;
        response.setHeader("Content-Security-Policy", buildPolicy(nonce));
    }

    private String buildPolicy(String nonce) {
        return "default-src 'self'; " +
               "script-src 'self' 'nonce-" + nonce + "'; " +
               "style-src 'self' 'nonce-" + nonce + "' https://fonts.googleapis.com; " +
               "font-src 'self' https://fonts.gstatic.com; " +
               "img-src 'self' data:; " +
               "connect-src 'self' ws: wss:; " +
               "frame-ancestors 'self';";
    }
}
