package com.groovycoder.dvsba.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@RestController
public class FetchController {

    private final UrlValidator urlValidator;

    public FetchController(UrlValidator urlValidator) {
        this.urlValidator = urlValidator;
    }

    @GetMapping("/fetch")
    public ResponseEntity<?> fetch(@RequestParam("url") String urlString) {
        if (!urlValidator.isSafe(urlString)) {
            return ResponseEntity.status(400).body("Blocked: URL is not allowed (SSRF protection)");
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            StringBuilder content = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
            }
            return ResponseEntity.ok("Fetched OK, length: " + content.length());
        } catch (Exception e) {
            return ResponseEntity.status(502).body("Fetch failed: " + e.getMessage());
        }
    }
}