package com.linkgrove.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${mail.sender:noreply@linkgrove.local}")
    private String sender;

    @Value("${public.base-url:http://localhost:3001}")
    private String publicBaseUrl;

    public void sendEmailVerification(String to, String token) {
        String verifyUrl = publicBaseUrl + "/verify-email?token=" + token;
        log.info("[DEV-EMAIL] To: {} Subject: Verify your email Body: {}", to, verifyUrl);
    }

    public void sendPasswordReset(String to, String token) {
        String resetUrl = publicBaseUrl + "/reset-password?token=" + token;
        log.info("[DEV-EMAIL] To: {} Subject: Reset your password Body: {}", to, resetUrl);
    }
}


