package com.geeknarrator.urlscanner.security;

import com.geeknarrator.urlscanner.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_Success() {
        // Given
        User user = createTestUser(1L, "test@example.com", "Test", "User");
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        User currentUser = SecurityUtils.getCurrentUser();

        // Then
        assertNotNull(currentUser);
        assertEquals(1L, currentUser.getId());
        assertEquals("test@example.com", currentUser.getEmail());
        assertEquals("Test", currentUser.getFirstName());
        assertEquals("User", currentUser.getLastName());
    }

    @Test
    void getCurrentUser_NoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> SecurityUtils.getCurrentUser());
        assertEquals("No authenticated user found", exception.getMessage());
    }

    @Test
    void getCurrentUser_NotAuthenticated() {
        // Given
        User user = createTestUser(1L, "test@example.com", "Test", "User");
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()) {
            @Override
            public boolean isAuthenticated() {
                return false;
            }
        };
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> SecurityUtils.getCurrentUser());
        assertEquals("No authenticated user found", exception.getMessage());
    }

    @Test
    void getCurrentUser_PrincipalNotUser() {
        // Given
        String principal = "string-principal"; // Not a User object
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> SecurityUtils.getCurrentUser());
        assertEquals("Invalid user type in security context", exception.getMessage());
    }

    @Test
    void getCurrentUserId_Success() {
        // Given
        User user = createTestUser(42L, "test@example.com", "Test", "User");
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        Long userId = SecurityUtils.getCurrentUserId();

        // Then
        assertNotNull(userId);
        assertEquals(42L, userId);
    }

    @Test
    void getCurrentUserId_NoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> SecurityUtils.getCurrentUserId());
        assertEquals("No authenticated user found", exception.getMessage());
    }

    @Test
    void getCurrentUserEmail_Success() {
        // Given
        User user = createTestUser(1L, "user@example.com", "Test", "User");
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        String userEmail = SecurityUtils.getCurrentUserEmail();

        // Then
        assertNotNull(userEmail);
        assertEquals("user@example.com", userEmail);
    }

    @Test
    void getCurrentUserEmail_NoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> SecurityUtils.getCurrentUserEmail());
        assertEquals("No authenticated user found", exception.getMessage());
    }

    @Test
    void getCurrentUser_AnonymousUser() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList());
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> SecurityUtils.getCurrentUser());
        assertEquals("Invalid user type in security context", exception.getMessage());
    }

    @Test
    void getCurrentUser_WithNullPrincipal() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(null, null, Collections.emptyList());
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> SecurityUtils.getCurrentUser());
        assertEquals("Invalid user type in security context", exception.getMessage());
    }

    @Test
    void securityUtils_AllMethodsConsistent() {
        // Given
        User user = createTestUser(123L, "consistent@example.com", "Consistent", "User");
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        User currentUser = SecurityUtils.getCurrentUser();
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();

        // Then
        assertEquals(currentUser.getId(), currentUserId);
        assertEquals(currentUser.getEmail(), currentUserEmail);
        assertEquals(123L, currentUserId);
        assertEquals("consistent@example.com", currentUserEmail);
    }

    @Test
    void getCurrentUser_MultipleCallsSameResult() {
        // Given
        User user = createTestUser(1L, "test@example.com", "Test", "User");
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        User user1 = SecurityUtils.getCurrentUser();
        User user2 = SecurityUtils.getCurrentUser();

        // Then
        assertSame(user, user1);
        assertSame(user, user2);
        assertEquals(user1.getId(), user2.getId());
        assertEquals(user1.getEmail(), user2.getEmail());
    }

    private User createTestUser(Long id, String email, String firstName, String lastName) {
        User user = new User(email, "password", firstName, lastName);
        user.setId(id);
        return user;
    }
}