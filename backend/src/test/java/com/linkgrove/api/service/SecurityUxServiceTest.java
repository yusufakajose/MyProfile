package com.linkgrove.api.service;

import com.linkgrove.api.model.EmailVerificationToken;
import com.linkgrove.api.model.PasswordResetToken;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.EmailVerificationTokenRepository;
import com.linkgrove.api.repository.PasswordResetTokenRepository;
import com.linkgrove.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityUxServiceTest {

    UserRepository userRepository;
    EmailVerificationTokenRepository emailRepo;
    PasswordResetTokenRepository resetRepo;
    EmailService emailService;
    PasswordEncoder passwordEncoder;
    SecurityUxService service;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        emailRepo = mock(EmailVerificationTokenRepository.class);
        resetRepo = mock(PasswordResetTokenRepository.class);
        emailService = mock(EmailService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new SecurityUxService(userRepository, emailRepo, resetRepo, emailService, passwordEncoder);
    }

    @Test
    void verifyEmail_returnsFalse_whenExpiredOrUsed() {
        User user = User.builder().id(1L).username("u").email("e").build();
        EmailVerificationToken expired = EmailVerificationToken.builder()
                .id(1L).user(user).token("t1")
                .createdAt(LocalDateTime.now().minusDays(3))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(emailRepo.findByToken("t1")).thenReturn(Optional.of(expired));
        assertFalse(service.verifyEmail("t1"));

        EmailVerificationToken used = EmailVerificationToken.builder()
                .id(2L).user(user).token("t2")
                .createdAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .usedAt(LocalDateTime.now().minusHours(1))
                .build();
        when(emailRepo.findByToken("t2")).thenReturn(Optional.of(used));
        assertFalse(service.verifyEmail("t2"));
    }

    @Test
    void resetPassword_returnsFalse_whenExpiredOrUsed() {
        User user = User.builder().id(1L).username("u").email("e").build();
        PasswordResetToken expired = PasswordResetToken.builder()
                .id(1L).user(user).token("rt1")
                .createdAt(LocalDateTime.now().minusHours(3))
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .build();
        when(resetRepo.findByToken("rt1")).thenReturn(Optional.of(expired));
        assertFalse(service.resetPassword("rt1", "NewP@ssw0rd!"));

        PasswordResetToken used = PasswordResetToken.builder()
                .id(2L).user(user).token("rt2")
                .createdAt(LocalDateTime.now().minusHours(3))
                .expiresAt(LocalDateTime.now().plusHours(2))
                .usedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(resetRepo.findByToken("rt2")).thenReturn(Optional.of(used));
        assertFalse(service.resetPassword("rt2", "NewP@ssw0rd!"));
    }
}


