package com.groovycoder.dvsba.security;

import io.jsonwebtoken.Claims;
import org.junit.Test;

import static org.junit.Assert.*;

public class AuthUnitTest {

    private JwtService newJwtService() {
        String secret = "Y2hhbmdlLXRoaXMtdG8tYS1yYW5kb20tMzItYnl0ZS1zZWNyZXQtMTIzNA==";
        return new JwtService(secret, 900000L, 604800000L, null);
    }

    @Test
    public void testGenerateAccessTokenContainsCorrectClaims() {
        JwtService jwt = newJwtService();
        String token = jwt.generateAccessToken("bosko", "USER");

        Claims claims = jwt.parseAccessToken(token);
        assertEquals("bosko", claims.getSubject());
        assertEquals("USER", claims.get("role", String.class));
        assertNotNull(claims.getExpiration());
    }

    @Test
    public void testValidTokenParsesSuccessfully() {
        JwtService jwt = newJwtService();
        String token = jwt.generateAccessToken("mirko", "ADMIN");
        Claims claims = jwt.parseAccessToken(token);
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test(expected = Exception.class)
    public void testTamperedTokenIsRejected() {
        JwtService jwt = newJwtService();
        String token = jwt.generateAccessToken("tonko", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        jwt.parseAccessToken(tampered);
    }

    @Test
    public void testUserStoreValidatesCorrectPassword() {
        UserStore store = new UserStore();
        assertTrue(store.validateCredentials("user", "password"));
        assertEquals("USER", store.getRole("user"));
    }

    @Test
    public void testUserStoreRejectsWrongPassword() {
        UserStore store = new UserStore();
        assertFalse(store.validateCredentials("user", "wrongpassword"));
    }

    @Test
    public void testUserStoreRejectsUnknownUser() {
        UserStore store = new UserStore();
        assertFalse(store.validateCredentials("nobody", "password"));
    }
}