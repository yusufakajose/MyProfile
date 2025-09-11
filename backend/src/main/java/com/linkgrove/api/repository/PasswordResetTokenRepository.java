package com.linkgrove.api.repository;

import com.linkgrove.api.model.PasswordResetToken;
import com.linkgrove.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findTopByUserOrderByCreatedAtDesc(User user);
}


