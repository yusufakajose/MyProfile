package com.linkgrove.api.repository;

import com.linkgrove.api.model.RefreshToken;
import com.linkgrove.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    long deleteByUserAndExpiresAtBefore(User user, LocalDateTime cutoff);
}


