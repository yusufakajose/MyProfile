package com.linkgrove.api.repository;

import com.linkgrove.api.model.EmailVerificationToken;
import com.linkgrove.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    Optional<EmailVerificationToken> findTopByUserOrderByCreatedAtDesc(User user);
}


