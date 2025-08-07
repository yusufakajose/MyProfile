package com.linkgrove.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Protected endpoint");
        response.put("username", authentication.getName());
        return ResponseEntity.ok(response);
    }
}
