package com.linkgrove.api.service;

import com.linkgrove.api.model.*;
import com.linkgrove.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityUxService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public void initiateEmailVerification(User user) {
        String token = generateToken();
        EmailVerificationToken evt = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .createdAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusDays(2))
                .build();
        emailVerificationTokenRepository.save(evt);
        emailService.sendEmailVerification(user.getEmail(), token);
    }

    @Transactional
    public void initiateEmailVerificationByUsername(String username) {
        userRepository.findByUsername(username).ifPresent(this::initiateEmailVerification);
    }

    @Transactional
    public boolean verifyEmail(String token) {
        return emailVerificationTokenRepository.findByToken(token).map(t -> {
            if (t.getUsedAt() != null || t.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                return false;
            }
            User user = t.getUser();
            user.setEmailVerified(true);
            user.setVerifiedAt(java.time.LocalDateTime.now());
            userRepository.save(user);
            t.setUsedAt(java.time.LocalDateTime.now());
            emailVerificationTokenRepository.save(t);
            return true;
        }).orElse(false);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = generateToken();
            PasswordResetToken prt = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .createdAt(java.time.LocalDateTime.now())
                    .expiresAt(java.time.LocalDateTime.now().plusHours(2))
                    .build();
            passwordResetTokenRepository.save(prt);
            emailService.sendPasswordReset(email, token);
        });
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        return passwordResetTokenRepository.findByToken(token).map(t -> {
            if (t.getUsedAt() != null || t.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                return false;
            }
            User user = t.getUser();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            t.setUsedAt(java.time.LocalDateTime.now());
            passwordResetTokenRepository.save(t);
            return true;
        }).orElse(false);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}


