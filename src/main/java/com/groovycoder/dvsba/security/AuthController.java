package com.groovycoder.dvsba.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserStore userStore;
    private final JwtService jwtService;

    public AuthController(UserStore userStore, JwtService jwtService) {
        this.userStore = userStore;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || !userStore.validateCredentials(username, password)) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        String role = userStore.getRole(username);
        String accessToken = jwtService.generateAccessToken(username, role);
        RefreshToken refreshToken = jwtService.createRefreshToken(username);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken.getToken());
        response.put("tokenType", "Bearer");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String presented = body.get("refreshToken");
        if (presented == null) {
            return ResponseEntity.status(400).body("Missing refreshToken");
        }
        try {
            RefreshToken rotated = jwtService.verifyAndRotate(presented);
            String role = userStore.getRole(rotated.getUsername());
            String newAccessToken = jwtService.generateAccessToken(rotated.getUsername(), role);

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", rotated.getToken());
            response.put("tokenType", "Bearer");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String presented = body.get("refreshToken");
        if (presented != null) {
            jwtService.revokeRefreshToken(presented);
        }
        return ResponseEntity.ok("Logged out successfully");
    }
}