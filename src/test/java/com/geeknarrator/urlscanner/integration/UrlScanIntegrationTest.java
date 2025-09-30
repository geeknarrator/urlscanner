package com.geeknarrator.urlscanner.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geeknarrator.urlscanner.controller.UrlScanController;
import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.entity.User;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import com.geeknarrator.urlscanner.repository.UserRepository;
import com.geeknarrator.urlscanner.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class UrlScanIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlScanRepository urlScanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser1;
    private User testUser2;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() {
        urlScanRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser1 = new User("user1@example.com", passwordEncoder.encode("password123"), "User", "One");
        testUser1 = userRepository.save(testUser1);
        user1Token = jwtUtil.generateToken(testUser1.getEmail());

        testUser2 = new User("user2@example.com", passwordEncoder.encode("password123"), "User", "Two");
        testUser2 = userRepository.save(testUser2);
        user2Token = jwtUtil.generateToken(testUser2.getEmail());
    }

    @Test
    void createScan_Success_WithAuthentication() throws Exception {
        // Given
        UrlScanController.CreateScanRequest request = new UrlScanController.CreateScanRequest();
        request.setUrl("https://example.com");

        // When & Then
        mockMvc.perform(post("/api/scans")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.url").value("https://example.com"))
                .andExpect(jsonPath("$.userId").value(testUser1.getId()))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void createScan_Unauthorized_NoToken() throws Exception {
        // Given
        UrlScanController.CreateScanRequest request = new UrlScanController.CreateScanRequest();
        request.setUrl("https://example.com");

        // When & Then
        mockMvc.perform(post("/api/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createScan_Unauthorized_InvalidToken() throws Exception {
        // Given
        UrlScanController.CreateScanRequest request = new UrlScanController.CreateScanRequest();
        request.setUrl("https://example.com");

        // When & Then
        mockMvc.perform(post("/api/scans")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllScans_Success_UserIsolation() throws Exception {
        // Given - Create scans for both users
        UrlScan user1Scan1 = new UrlScan("https://user1-scan1.com", testUser1.getId());
        UrlScan user1Scan2 = new UrlScan("https://user1-scan2.com", testUser1.getId());
        UrlScan user2Scan = new UrlScan("https://user2-scan.com", testUser2.getId());

        urlScanRepository.save(user1Scan1);
        urlScanRepository.save(user1Scan2);
        urlScanRepository.save(user2Scan);

        // When & Then - User1 should only see their scans
        mockMvc.perform(get("/api/scans")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].userId").value(testUser1.getId()))
                .andExpect(jsonPath("$.content[1].userId").value(testUser1.getId()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllScans_WithPagination() throws Exception {
        // Given - Create multiple scans for user1
        for (int i = 1; i <= 15; i++) {
            UrlScan scan = new UrlScan("https://example" + i + ".com", testUser1.getId());
            urlScanRepository.save(scan);
        }

        // When & Then - First page
        mockMvc.perform(get("/api/scans")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(15));
    }

    @Test
    void getScanById_Success_OwnerAccess() throws Exception {
        // Given
        UrlScan scan = new UrlScan("https://example.com", testUser1.getId());
        scan = urlScanRepository.save(scan);

        // When & Then
        mockMvc.perform(get("/api/scans/" + scan.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scan.getId()));
    }

    @Test
    void getScanById_Forbidden_DifferentUser() throws Exception {
        // Given
        UrlScan scan = new UrlScan("https://example.com", testUser1.getId());
        scan = urlScanRepository.save(scan);

        // When & Then - User2 tries to access User1's scan
        mockMvc.perform(get("/api/scans/" + scan.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteScan_Success_OwnerAccess() throws Exception {
        // Given
        UrlScan scan = new UrlScan("https://example.com", testUser1.getId());
        scan = urlScanRepository.save(scan);
        Long scanId = scan.getId();

        // When & Then
        mockMvc.perform(delete("/api/scans/" + scanId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        assertFalse(urlScanRepository.existsById(scanId));
    }
}
