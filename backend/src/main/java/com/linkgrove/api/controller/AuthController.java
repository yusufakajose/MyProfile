package com.linkgrove.api.controller;

import com.linkgrove.api.dto.AuthResponse;
import com.linkgrove.api.dto.RefreshRequest;
import com.linkgrove.api.dto.LoginRequest;
import com.linkgrove.api.dto.RegisterRequest;
import com.linkgrove.api.dto.InitiatePasswordResetRequest;
import com.linkgrove.api.dto.PerformPasswordResetRequest;
import com.linkgrove.api.dto.VerifyEmailRequest;
import com.linkgrove.api.service.AuthService;
import com.linkgrove.api.model.User;
import com.linkgrove.api.service.SecurityUxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final SecurityUxService securityUxService;
    private final com.linkgrove.api.repository.UserRepository userRepository;
    private final com.linkgrove.api.repository.EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final com.linkgrove.api.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/initiate-password-reset")
    public ResponseEntity<Void> initiatePasswordReset(@Valid @RequestBody InitiatePasswordResetRequest request) {
        securityUxService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> performPasswordReset(@Valid @RequestBody PerformPasswordResetRequest request) {
        boolean ok = securityUxService.resetPassword(request.getToken(), request.getNewPassword());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @PostMapping("/initiate-email-verification")
    public ResponseEntity<Void> initiateEmailVerification(@RequestParam("username") String username) {
        securityUxService.initiateEmailVerificationByUsername(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        boolean ok = securityUxService.verifyEmail(request.getToken());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running with database");
    }

    @GetMapping("/dev/latest-email-verification-token")
    public ResponseEntity<String> latestEmailVerificationToken(@RequestParam("username") String username) {
        if (!isDevEnabled()) return ResponseEntity.status(404).build();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(404).build();
        return emailVerificationTokenRepository.findTopByUserOrderByCreatedAtDesc(user)
                .map(t -> ResponseEntity.ok(t.getToken()))
                .orElse(ResponseEntity.status(404).build());
    }

    @GetMapping("/dev/latest-password-reset-token")
    public ResponseEntity<String> latestPasswordResetToken(@RequestParam("email") String email) {
        if (!isDevEnabled()) return ResponseEntity.status(404).build();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(404).build();
        return passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(user)
                .map(t -> ResponseEntity.ok(t.getToken()))
                .orElse(ResponseEntity.status(404).build());
    }

    private boolean isDevEnabled() {
        String flag = System.getenv("ENABLE_DEV_TOKEN_ENDPOINTS");
        return flag != null && (flag.equalsIgnoreCase("1") || flag.equalsIgnoreCase("true"));
    }
}
