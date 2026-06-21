package com.groovycoder.dvsba.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserStore {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    private final Map<String, String> users = new HashMap<>();
    private final Map<String, String> roles = new HashMap<>();

    public UserStore() {
        users.put("user", encoder.encode("password"));
        roles.put("user", "USER");

        users.put("admin", encoder.encode("admin123"));
        roles.put("admin", "ADMIN");
    }

    public boolean validateCredentials(String username, String rawPassword) {
        String storedHash = users.get(username);
        if (storedHash == null) {
            return false;
        }
        return encoder.matches(rawPassword, storedHash);
    }

    public String getRole(String username) {
        return roles.get(username);
    }
}