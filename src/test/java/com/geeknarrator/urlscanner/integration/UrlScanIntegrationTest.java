package com.geeknarrator.urlscanner.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geeknarrator.urlscanner.controller.UrlScanController;
import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.entity.User;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import com.geeknarrator.urlscanner.repository.UserRepository;
import com.geeknarrator.urlscanner.security.JwtUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UrlScanIntegrationTest {

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
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        // Verify scan was created in database
        List<UrlScan> scans = urlScanRepository.findAll();
        assertEquals(1, scans.size());
        UrlScan savedScan = scans.get(0);
        assertEquals("https://example.com", savedScan.getUrl());
        assertEquals(testUser1.getId(), savedScan.getUserId());
        assertEquals(UrlScan.ScanStatus.SUBMITTED, savedScan.getStatus());
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
                .andExpect(status().isForbidden());

        // Verify no scan was created
        assertEquals(0, urlScanRepository.count());
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
                .andExpect(status().isForbidden());

        // Verify no scan was created
        assertEquals(0, urlScanRepository.count());
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

        // User2 should only see their scan
        mockMvc.perform(get("/api/scans")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(testUser2.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
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
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));

        // Second page
        mockMvc.perform(get("/api/scans")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(10));
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
                .andExpect(jsonPath("$.id").value(scan.getId()))
                .andExpect(jsonPath("$.url").value("https://example.com"))
                .andExpect(jsonPath("$.userId").value(testUser1.getId()));
    }

    @Test
    void getScanById_Forbidden_DifferentUser() throws Exception {
        // Given
        UrlScan scan = new UrlScan("https://example.com", testUser1.getId());
        scan = urlScanRepository.save(scan);

        // When & Then - User2 tries to access User1's scan
        mockMvc.perform(get("/api/scans/" + scan.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound()); // Should return 404 for security (not 403)
    }

    @Test
    void getScanById_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/scans/999")
                        .header("Authorization", "Bearer " + user1Token))
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

        // Verify scan was deleted
        assertFalse(urlScanRepository.existsById(scanId));
    }

    @Test
    void deleteScan_Forbidden_DifferentUser() throws Exception {
        // Given
        UrlScan scan = new UrlScan("https://example.com", testUser1.getId());
        scan = urlScanRepository.save(scan);
        Long scanId = scan.getId();

        // When & Then - User2 tries to delete User1's scan
        mockMvc.perform(delete("/api/scans/" + scanId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound()); // Should return 404 for security

        // Verify scan was not deleted
        assertTrue(urlScanRepository.existsById(scanId));
    }

    @Test
    void deleteScan_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/scans/999")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void urlScan_InvalidUrl_ValidationError() throws Exception {
        // Given
        UrlScanController.CreateScanRequest request = new UrlScanController.CreateScanRequest();
        request.setUrl("invalid-url");

        // When & Then
        mockMvc.perform(post("/api/scans")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify no scan was created
        assertEquals(0, urlScanRepository.count());
    }

    @Test
    void fullUrlScanFlow_CreateReadDelete() throws Exception {
        // Step 1: Create a scan
        UrlScanController.CreateScanRequest createRequest = new UrlScanController.CreateScanRequest();
        createRequest.setUrl("https://fullflow.com");

        String createResponse = mockMvc.perform(post("/api/scans")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdScan = objectMapper.readTree(createResponse);
        Long scanId = createdScan.get("id").asLong();

        // Step 2: Read the scan
        mockMvc.perform(get("/api/scans/" + scanId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scanId))
                .andExpect(jsonPath("$.url").value("https://fullflow.com"))
                .andExpect(jsonPath("$.userId").value(testUser1.getId()));

        // Step 3: List scans (should include our scan)
        mockMvc.perform(get("/api/scans")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(scanId));

        // Step 4: Delete the scan
        mockMvc.perform(delete("/api/scans/" + scanId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        // Step 5: Verify deletion
        mockMvc.perform(get("/api/scans/" + scanId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/scans")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

}