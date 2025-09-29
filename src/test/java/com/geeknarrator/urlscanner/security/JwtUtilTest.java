package com.geeknarrator.urlscanner.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "testSecretKeyThatIsLongEnoughForHS256Algorithm";
    private final long testExpiration = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set private fields using reflection for testing
        ReflectionTestUtils.setField(jwtUtil, "secretKey", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", testExpiration);
    }

    @Test
    void generateToken_Success() {
        // Given
        String email = "test@example.com";

        // When
        String token = jwtUtil.generateToken(email);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts separated by dots
    }

    @Test
    void generateToken_NullEmail() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.generateToken(null));
    }

    @Test
    void generateToken_EmptyEmail() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.generateToken(""));
    }

    @Test
    void extractUsername_Success() {
        // Given
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        // When
        String extractedUsername = jwtUtil.extractUsername(token);

        // Then
        assertEquals(email, extractedUsername);
    }

    @Test
    void extractUsername_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThrows(MalformedJwtException.class, () -> jwtUtil.extractUsername(invalidToken));
    }

    @Test
    void extractUsername_NullToken() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.extractUsername(null));
    }

    @Test
    void extractUsername_EmptyToken() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.extractUsername(""));
    }

    @Test
    void extractExpiration_Success() {
        // Given
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
        // Should expire approximately 24 hours from now (allowing 1 minute tolerance)
        long expectedExpiration = System.currentTimeMillis() + testExpiration;
        long actualExpiration = expiration.getTime();
        assertTrue(Math.abs(expectedExpiration - actualExpiration) < 60000); // 1 minute tolerance
    }

    @Test
    void isTokenExpired_ValidToken() {
        // Given
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        // When
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void isTokenExpired_ExpiredToken() {
        // Given - Create a token that expires immediately
        JwtUtil shortExpirationJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "secretKey", testSecret);
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "jwtExpiration", -1L); // Expired immediately

        String email = "test@example.com";
        String expiredToken = shortExpirationJwtUtil.generateToken(email);

        // When & Then
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.isTokenExpired(expiredToken));
    }

    @Test
    void validateToken_ValidToken() {
        // Given
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        // When
        boolean isValid = jwtUtil.validateToken(token, email);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_ValidTokenWrongEmail() {
        // Given
        String email = "test@example.com";
        String wrongEmail = "wrong@example.com";
        String token = jwtUtil.generateToken(email);

        // When
        boolean isValid = jwtUtil.validateToken(token, wrongEmail);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_ExpiredToken() {
        // Given - Create a token that expires immediately
        JwtUtil shortExpirationJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "secretKey", testSecret);
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "jwtExpiration", -1L); // Expired immediately

        String email = "test@example.com";
        String expiredToken = shortExpirationJwtUtil.generateToken(email);

        // When
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.validateToken(expiredToken, email));
    }

    @Test
    void validateToken_InvalidToken() {
        // Given
        String email = "test@example.com";
        String invalidToken = "invalid.token.format";

        // When
        assertThrows(MalformedJwtException.class,() ->  jwtUtil.validateToken(invalidToken, email));
    }

    @Test
    void validateToken_TokenWithWrongSignature() {
        // Given
        String email = "test@example.com";
        
        // Create token with different secret
        JwtUtil differentSecretJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(differentSecretJwtUtil, "secretKey", "differentSecretKeyThatIsLongEnoughForHS256");
        ReflectionTestUtils.setField(differentSecretJwtUtil, "jwtExpiration", testExpiration);
        
        String tokenWithWrongSignature = differentSecretJwtUtil.generateToken(email);

        // When
        assertThrows(SignatureException.class,() ->  jwtUtil.validateToken(tokenWithWrongSignature, email));
    }

    @Test
    void validateToken_NullToken() {
        // Given
        String email = "test@example.com";

        // When
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.validateToken(null, email));
    }

    @Test
    void validateToken_NullEmail() {
        // Given
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        // When
        boolean isValid = jwtUtil.validateToken(token, null);

        // Then
        assertFalse(isValid);
    }


    @Test
    void extractClaim_Success() {
        // Given
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        // When
        String subject = jwtUtil.extractClaim(token, Claims::getSubject);
        Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);
        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);

        // Then
        assertEquals(email, subject);
        assertNotNull(expiration);
        assertNotNull(issuedAt);
        assertTrue(expiration.after(issuedAt));
    }
}