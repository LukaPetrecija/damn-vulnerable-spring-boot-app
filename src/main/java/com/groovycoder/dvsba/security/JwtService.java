package com.groovycoder.dvsba.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

private final SecretKey key;
private final long accessTokenExpirationMs;

public JwtService(@Value("${jwt.secret}") String base64Secret, @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessTokenExpirationMs = accessTokenExpirationMs;
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
}