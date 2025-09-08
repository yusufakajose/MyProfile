package com.linkgrove.api.service;

import com.linkgrove.api.dto.AuthResponse;
import com.linkgrove.api.dto.LoginRequest;
import com.linkgrove.api.dto.RegisterRequest;
import com.linkgrove.api.exception.UnauthorizedException;
import com.linkgrove.api.model.RefreshToken;
import com.linkgrove.api.model.Role;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.RefreshTokenRepository;
import com.linkgrove.api.repository.RoleRepository;
import com.linkgrove.api.repository.UserRepository;
import com.linkgrove.api.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LockoutService lockoutService;
    private final JwtUtil jwtUtil;
    private final SecurityUxService securityUxService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Create USER role if it doesn't exist
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    return roleRepository.save(role);
                });

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new HashSet<>())
                .build();

        user.getRoles().add(userRole);
        user = userRepository.save(user);

        // Send email verification link
        securityUxService.initiateEmailVerification(user);

        String token = jwtUtil.generateToken(user.getUsername());
        String refresh = generateAndStoreRefreshToken(user);
        return new AuthResponse(token, refresh, user.getUsername(), user.getEmail());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (lockoutService.isLocked(request.getUsername())) {
            throw new UnauthorizedException("Too many failed attempts. Try again later.");
        }
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            lockoutService.onFailedAttempt(request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }

        lockoutService.onSuccess(request.getUsername());

        String token = jwtUtil.generateToken(user.getUsername());
        String refresh = generateAndStoreRefreshToken(user);
        return new AuthResponse(token, refresh, user.getUsername(), user.getEmail());
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        String hash = sha256(refreshToken);
        RefreshToken found = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (Boolean.TRUE.equals(found.getRevoked()) || found.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }
        User user = found.getUser();
        // rotate: revoke current and issue new
        found.setRevoked(true);
        found.setLastUsedAt(java.time.LocalDateTime.now());
        refreshTokenRepository.save(found);
        String newRaw = generateSecureToken();
        String newHash = sha256(newRaw);
        found.setReplacedBy(newHash);

        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(newHash)
                .createdAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusDays(30))
                .revoked(false)
                .ip(getCurrentRequestIp())
                .userAgent(getCurrentRequestUserAgent())
                .lastUsedAt(java.time.LocalDateTime.now())
                .build()
                ;
        refreshTokenRepository.save(rt);

        String newAccess = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(newAccess, newRaw, user.getUsername(), user.getEmail());
    }

    private String generateAndStoreRefreshToken(User user) {
        String raw = generateSecureToken();
        String hash = sha256(raw);
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .createdAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusDays(30))
                .revoked(false)
                .ip(getCurrentRequestIp())
                .userAgent(getCurrentRequestUserAgent())
                .lastUsedAt(java.time.LocalDateTime.now())
                .build();
        refreshTokenRepository.save(rt);
        // cleanup old expired tokens opportunistically
        refreshTokenRepository.deleteByUserAndExpiresAtBefore(user, java.time.LocalDateTime.now().minusDays(1));
        return raw;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String sha256(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("hash error");
        }
    }

    private String getCurrentRequestIp() {
        try {
            jakarta.servlet.http.HttpServletRequest req = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest() : null;
            if (req == null) return null;
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                int comma = xff.indexOf(',');
                return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
            }
            String real = req.getHeader("X-Real-IP");
            if (real != null && !real.isBlank()) return real.trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentRequestUserAgent() {
        try {
            jakarta.servlet.http.HttpServletRequest req = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest() : null;
            return req != null ? req.getHeader("User-Agent") : null;
        } catch (Exception e) { return null; }
    }
}