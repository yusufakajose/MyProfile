package com.linkgrove.api.controller;

import com.linkgrove.api.dto.ProfileResponse;
import com.linkgrove.api.dto.UpdateProfileRequest;
import com.linkgrove.api.dto.UpdateUsernameRequest;
import com.linkgrove.api.dto.UpdateUsernameResponse;
import com.linkgrove.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        ProfileResponse profile = userService.getProfile(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        ProfileResponse updated = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/username")
    public ResponseEntity<UpdateUsernameResponse> updateUsername(
            Authentication authentication,
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        UpdateUsernameResponse response = userService.updateUsername(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }
}
