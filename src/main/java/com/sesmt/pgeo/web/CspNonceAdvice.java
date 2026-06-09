package com.sesmt.pgeo.web;

import com.sesmt.pgeo.security.CspNonceFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CspNonceAdvice {

    @ModelAttribute("cspNonce")
    public String cspNonce(HttpServletRequest request) {
        Object nonce = request.getAttribute(CspNonceFilter.NONCE_ATTR);
        return nonce != null ? nonce.toString() : "";
    }
}
