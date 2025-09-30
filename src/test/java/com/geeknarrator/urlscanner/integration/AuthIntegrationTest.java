package com.geeknarrator.urlscanner.integration;

import com.geeknarrator.urlscanner.controller.AuthController;
import com.geeknarrator.urlscanner.entity.User;
import com.geeknarrator.urlscanner.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_Success_CreatesUserInDatabase() throws Exception {
        // Given
        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"));

        // Verify user was created in database
        assertTrue(userRepository.existsByEmail("newuser@example.com"));
        User savedUser = userRepository.findByEmail("newuser@example.com").orElse(null);
        assertNotNull(savedUser);
        assertEquals("newuser@example.com", savedUser.getEmail());
        assertEquals("New", savedUser.getFirstName());
        assertEquals("User", savedUser.getLastName());
        assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()));
        assertTrue(savedUser.getIsActive());
    }

    @Test
    void register_EmailAlreadyExists_ReturnsError() throws Exception {
        // Given - Create existing user
        User existingUser = new User("existing@example.com", passwordEncoder.encode("password"), "Existing", "User");
        userRepository.save(existingUser);

        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already in use!"));

        // Verify only one user exists with this email
        assertEquals(1, userRepository.findAll().size());
    }

    @Test
    void register_InvalidEmail_ReturnsValidationError() throws Exception {
        // Given
        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify no user was created
        assertEquals(0, userRepository.count());
    }

    @Test
    void register_ShortPassword_ReturnsValidationError() throws Exception {
        // Given
        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("123"); // Too short
        request.setFirstName("Test");
        request.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify no user was created
        assertEquals(0, userRepository.count());
    }

    @Test
    void login_Success_ValidCredentials() throws Exception {
        // Given - Create user in database
        User user = new User("login@example.com", passwordEncoder.encode("password123"), "Login", "User");
        user = userRepository.save(user);

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("login@example.com");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.firstName").value("Login"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    void login_InvalidPassword_ReturnsError() throws Exception {
        // Given - Create user in database
        User user = new User("login@example.com", passwordEncoder.encode("correctpassword"), "Login", "User");
        userRepository.save(user);

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("login@example.com");
        request.setPassword("wrongpassword");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password!"));
    }

    @Test
    void login_UserNotExists_ReturnsError() throws Exception {
        // Given
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password!"));
    }

    @Test
    void login_InactiveUser_ReturnsError() throws Exception {
        // Given - Create inactive user
        User user = new User("inactive@example.com", passwordEncoder.encode("password123"), "Inactive", "User");
        user.setIsActive(false);
        userRepository.save(user);

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("inactive@example.com");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password!"));
    }

    @Test
    void login_InvalidEmailFormat_ReturnsValidationError() throws Exception {
        // Given
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullAuthFlow_RegisterThenLogin_Success() throws Exception {
        // Step 1: Register
        AuthController.RegisterRequest registerRequest = new AuthController.RegisterRequest();
        registerRequest.setEmail("flow@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Flow");
        registerRequest.setLastName("User");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("flow@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify user exists in database
        assertTrue(userRepository.existsByEmail("flow@example.com"));

        // Step 2: Login with same credentials
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setEmail("flow@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("flow@example.com"))
                .andExpect(jsonPath("$.firstName").value("Flow"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    void auth_CaseSensitiveEmail() throws Exception {
        // Given
        AuthController.RegisterRequest registerRequest = new AuthController.RegisterRequest();
        registerRequest.setEmail("Test@Example.Com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        // Register with mixed case email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Try to login with different case
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }
}