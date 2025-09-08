package com.linkgrove.api.controller;

import com.linkgrove.api.dto.AuthResponse;
import com.linkgrove.api.dto.RefreshRequest;
import com.linkgrove.api.dto.LoginRequest;
import com.linkgrove.api.dto.RegisterRequest;
import com.linkgrove.api.dto.InitiatePasswordResetRequest;
import com.linkgrove.api.dto.PerformPasswordResetRequest;
import com.linkgrove.api.dto.VerifyEmailRequest;
import com.linkgrove.api.service.AuthService;
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
}
