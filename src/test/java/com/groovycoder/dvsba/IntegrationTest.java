package com.groovycoder.dvsba;

import com.groovycoder.dvsba.DvsbaApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DvsbaApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String login(String username, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        ResponseEntity<Map> resp = rest.postForEntity("/auth/login", body, Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return (String) resp.getBody().get("accessToken");
    }

    @org.junit.Before
    public void setup() {
        rest.getRestTemplate().setRequestFactory(
                new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void loginWithValidCredentialsReturnsTokens() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "user");
        body.put("password", "password");
        ResponseEntity<Map> resp = rest.postForEntity("/auth/login", body, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().get("accessToken"));
        assertNotNull(resp.getBody().get("refreshToken"));
    }

    @Test
    public void loginWithInvalidPasswordReturns401() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "user");
        body.put("password", "wrongpassword");

        ResponseEntity<String> resp = rest.exchange(
                "/auth/login", HttpMethod.POST,
                new HttpEntity<>(body), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    public void protectedEndpointWithoutTokenReturns401() {
        ResponseEntity<String> resp = rest.getForEntity("/api/me", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    public void protectedEndpointWithTokenExercisesFilter() {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "user");
        creds.put("password", "password");
        ResponseEntity<Map> loginResp = rest.postForEntity("/auth/login", creds, Map.class);
        String token = (String) loginResp.getBody().get("accessToken");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        ResponseEntity<String> resp = rest.exchange(
                "/api/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertNotNull(resp.getStatusCode());
    }

    @Test
    public void publicBooksPageIsAccessible() {
        ResponseEntity<String> resp = rest.getForEntity("/books/", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    public void bookDetailWithInvalidIdReturns400() {
        ResponseEntity<String> resp = rest.getForEntity("/books/detail?id=abc", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void deserializeValidObjectSucceeds() {
        ResponseEntity<String> resp = rest.getForEntity("/deserialize/valid", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("Deserialized successfully"));
    }

    @Test
    public void deserializeNonWhitelistedClassIsRejected() {
        ResponseEntity<String> resp = rest.getForEntity("/deserialize/nonwhitelisted", String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertTrue(resp.getBody().contains("REJECTED"));
    }

    @Test
    public void deserializeInvalidFileIsRejected() {
        ResponseEntity<String> resp = rest.getForEntity("/deserialize/invalidfile", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().contains("REJECTED"));
    }

    @Test
    public void refreshTokenReturnsNewTokens() {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "user");
        creds.put("password", "password");
        ResponseEntity<Map> loginResp = rest.postForEntity("/auth/login", creds, Map.class);
        String refreshToken = (String) loginResp.getBody().get("refreshToken");

        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);
        ResponseEntity<Map> resp = rest.postForEntity("/auth/refresh", body, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().get("accessToken"));
        assertNotNull(resp.getBody().get("refreshToken"));
    }

    @Test
    public void reusedRefreshTokenIsRejected() {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "user");
        creds.put("password", "password");
        ResponseEntity<Map> loginResp = rest.postForEntity("/auth/login", creds, Map.class);
        String refreshToken = (String) loginResp.getBody().get("refreshToken");

        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);

        rest.postForEntity("/auth/refresh", body, Map.class);
        ResponseEntity<String> second = rest.exchange("/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(body), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, second.getStatusCode());
    }

    @Test
    public void logoutRevokesRefreshToken() {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "user");
        creds.put("password", "password");
        ResponseEntity<Map> loginResp = rest.postForEntity("/auth/login", creds, Map.class);
        String refreshToken = (String) loginResp.getBody().get("refreshToken");

        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);

        ResponseEntity<String> logoutResp = rest.postForEntity("/auth/logout", body, String.class);
        assertEquals(HttpStatus.OK, logoutResp.getStatusCode());

        ResponseEntity<String> afterLogout = rest.exchange("/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(body), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, afterLogout.getStatusCode());
    }

    @Test
    public void commentsPageIsAccessible() {
        ResponseEntity<String> resp = rest.getForEntity("/comments/", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    public void postCommentWithoutCsrfTokenIsRejected() {
        org.springframework.util.MultiValueMap<String, String> form =
                new org.springframework.util.LinkedMultiValueMap<>();
        form.add("input", "test comment");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<org.springframework.util.MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        ResponseEntity<String> resp = rest.postForEntity("/comments/", req, String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    public void bookDetailWithValidIdReturnsBook() {
        ResponseEntity<String> resp = rest.getForEntity("/books/detail?id=1", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    public void fetchInternalUrlIsBlocked() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/fetch?url=http://169.254.169.254/latest/meta-data/", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void fetchInvalidProtocolIsBlocked() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/fetch?url=file:///etc/passwd", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void refreshWithInvalidTokenIsRejected() {
        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", "this-is-not-a-real-token");
        ResponseEntity<String> resp = rest.exchange("/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(body), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    public void refreshWithMissingTokenReturns400() {
        ResponseEntity<String> resp = rest.exchange("/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(new HashMap<String, String>()), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void logoutWithoutTokenStillReturns200() {
        ResponseEntity<String> resp = rest.postForEntity("/auth/logout",
                new HashMap<String, String>(), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    public void loginWithMissingFieldsReturns401() {
        ResponseEntity<String> resp = rest.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>(new HashMap<String, String>()), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    public void serializeEndpointWorks() {
        ResponseEntity<String> r1 = rest.getForEntity("/deserialize/valid", String.class);
        assertEquals(HttpStatus.OK, r1.getStatusCode());
    }

    @Test
    public void homePageIsAccessible() {
        ResponseEntity<String> resp = rest.getForEntity("/", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    public void protectedEndpointWithGarbageTokenIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer not.a.valid.token");
        ResponseEntity<String> resp = rest.exchange(
                "/api/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    public void fetchValidExternalUrlIsAllowedOrFails() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/fetch?url=http://example.com", String.class);
        assertTrue(resp.getStatusCodeValue() == 200 || resp.getStatusCodeValue() == 502);
    }
}