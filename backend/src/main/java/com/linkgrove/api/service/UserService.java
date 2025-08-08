package com.linkgrove.api.service;

import com.linkgrove.api.dto.ProfileResponse;
import com.linkgrove.api.dto.UpdateProfileRequest;
import com.linkgrove.api.dto.UpdateUsernameRequest;
import com.linkgrove.api.dto.UpdateUsernameResponse;
import com.linkgrove.api.util.JwtUtil;
import com.linkgrove.api.model.User;
import com.linkgrove.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .profileImageUrl(user.getProfileImageUrl())
                .themePrimaryColor(user.getThemePrimaryColor())
                .themeAccentColor(user.getThemeAccentColor())
                .themeBackgroundColor(user.getThemeBackgroundColor())
                .themeTextColor(user.getThemeTextColor())
                .build();
    }

    @Transactional
    public ProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getThemePrimaryColor() != null) {
            user.setThemePrimaryColor(request.getThemePrimaryColor());
        }
        if (request.getThemeAccentColor() != null) {
            user.setThemeAccentColor(request.getThemeAccentColor());
        }
        if (request.getThemeBackgroundColor() != null) {
            user.setThemeBackgroundColor(request.getThemeBackgroundColor());
        }
        if (request.getThemeTextColor() != null) {
            user.setThemeTextColor(request.getThemeTextColor());
        }

        User saved = userRepository.save(user);
        return ProfileResponse.builder()
                .username(saved.getUsername())
                .email(saved.getEmail())
                .displayName(saved.getDisplayName())
                .bio(saved.getBio())
                .profileImageUrl(saved.getProfileImageUrl())
                .themePrimaryColor(saved.getThemePrimaryColor())
                .themeAccentColor(saved.getThemeAccentColor())
                .themeBackgroundColor(saved.getThemeBackgroundColor())
                .themeTextColor(saved.getThemeTextColor())
                .build();
    }

    @Transactional
    public UpdateUsernameResponse updateUsername(String currentUsername, UpdateUsernameRequest request) {
        String desired = request.getNewUsername();
        if (desired == null || desired.isBlank()) {
            throw new RuntimeException("New username required");
        }
        if (userRepository.findByUsername(desired).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(desired);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return new UpdateUsernameResponse(token, user.getUsername());
    }
}


