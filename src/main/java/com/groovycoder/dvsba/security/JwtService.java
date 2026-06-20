package com.groovycoder.dvsba.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

private final SecretKey key;
private final long accessTokenExpirationMs;
private final long refreshTokenExpirationMs;
private final RefreshTokenRepository refreshTokenRepository;
private final SecureRandom secureRandom = new SecureRandom();

public JwtService(@Value("${jwt.secret}") String base64Secret,
    @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
    @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
    RefreshTokenRepository refreshTokenRepository) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshTokenRepository = refreshTokenRepository;
}

public String generateAccessToken(String username, String role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + accessTokenExpirationMs);
    return Jwts.builder()
    .subject(username)    
    .claim("role", role)
    .issuedAt(now)
    .expiration(expiry)
    .signWith(key, Jwts.SIG.HS256)
    .compact();
    }


public RefreshToken createRefreshToken(String username) {
    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

    Instant expiry = Instant.now().plusMillis(refreshTokenExpirationMs);

    RefreshToken refreshToken = new RefreshToken(token, username, expiry);
    return refreshTokenRepository.save(refreshToken);
}

public RefreshToken verifyAndRotate(String presentedToken) {
        RefreshToken existing = refreshTokenRepository.findByToken(presentedToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (existing.isRevoked()) {
            revokeAllUserTokens(existing.getUsername());
            throw new RuntimeException("Refresh token reuse detected! All sessions invalidated!");
        }

        if (existing.getExpiryDate().isBefore(Instant.now())) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new RuntimeException("Refresh token expired!");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);
        return createRefreshToken(existing.getUsername());
    }

    public void revokeAllUserTokens(String username) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUsername(username);
        for (RefreshToken t : tokens) {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        }
    }

    public void revokeRefreshToken(String presentedToken) {
        refreshTokenRepository.findByToken(presentedToken).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

public Claims parseAccessToken(String token) {
    return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
}