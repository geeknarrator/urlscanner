package com.geeknarrator.urlscanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import com.geeknarrator.urlscanner.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UrlScanControllerTest {

    @Mock
    private UrlScanRepository urlScanRepository;

    @InjectMocks
    private UrlScanController urlScanController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(urlScanController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createScan_Success() throws Exception {
        // Given
        UrlScanController.CreateScanRequest request = new UrlScanController.CreateScanRequest();
        request.setUrl("https://example.com");

        UrlScan savedScan = new UrlScan("https://example.com", 1L);
        savedScan.setId(1L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(urlScanRepository.save(any(UrlScan.class))).thenReturn(savedScan);

            // When & Then
            mockMvc.perform(post("/api/scans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.url").value("https://example.com"))
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.status").value("SUBMITTED"));

            verify(urlScanRepository).save(any(UrlScan.class));
        }
    }

    @Test
    void createScan_InvalidUrl() throws Exception {
        // Given
        UrlScanController.CreateScanRequest request = new UrlScanController.CreateScanRequest();
        request.setUrl("invalid-url");

        // When & Then
        mockMvc.perform(post("/api/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(urlScanRepository, never()).save(any());
    }

    @Test
    void createScan_EmptyUrl() throws Exception {
        // Given
        UrlScanController.CreateScanRequest request = new UrlScanController.CreateScanRequest();
        request.setUrl("");

        // When & Then
        mockMvc.perform(post("/api/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(urlScanRepository, never()).save(any());
    }

    @Test
    void getAllScans_Success() throws Exception {
        // Given
        List<UrlScan> scans = Arrays.asList(
                createUrlScan(1L, "https://example1.com", 1L),
                createUrlScan(2L, "https://example2.com", 1L)
        );
        Page<UrlScan> page = new PageImpl<>(scans, PageRequest.of(0, 10), 2);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(urlScanRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/scans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(1L))
                    .andExpect(jsonPath("$.content[0].url").value("https://example1.com"))
                    .andExpect(jsonPath("$.content[1].id").value(2L))
                    .andExpect(jsonPath("$.content[1].url").value("https://example2.com"))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.number").value(0));

            verify(urlScanRepository).findByUserId(eq(1L), any(Pageable.class));
        }
    }

    @Test
    void getAllScans_WithPagination() throws Exception {
        // Given
        List<UrlScan> scans = Arrays.asList(
                createUrlScan(3L, "https://example3.com", 1L),
                createUrlScan(4L, "https://example4.com", 1L)
        );
        Page<UrlScan> page = new PageImpl<>(scans, PageRequest.of(1, 2), 10);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(urlScanRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/scans")
                            .param("page", "1")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(10))
                    .andExpect(jsonPath("$.totalPages").value(5))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.number").value(1));

            verify(urlScanRepository).findByUserId(eq(1L), any(Pageable.class));
        }
    }

    @Test
    void getScanById_Success() throws Exception {
        // Given
        UrlScan scan = createUrlScan(1L, "https://example.com", 1L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(urlScanRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(scan));

            // When & Then
            mockMvc.perform(get("/api/scans/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.url").value("https://example.com"))
                    .andExpect(jsonPath("$.userId").value(1L));

            verify(urlScanRepository).findByIdAndUserId(1L, 1L);
        }
    }

    @Test
    void getScanById_NotFound() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(urlScanRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/scans/999"))
                    .andExpect(status().isNotFound());

            verify(urlScanRepository).findByIdAndUserId(999L, 1L);
        }
    }

    @Test
    void getScanById_WrongUser() throws Exception {
        // Given - User 2 trying to access scan belonging to user 1
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            when(urlScanRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/scans/1"))
                    .andExpect(status().isNotFound());

            verify(urlScanRepository).findByIdAndUserId(1L, 2L);
        }
    }

    @Test
    void deleteScan_Success() throws Exception {
        // Given
        UrlScan scan = createUrlScan(1L, "https://example.com", 1L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(urlScanRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(scan));

            // When & Then
            mockMvc.perform(delete("/api/scans/1"))
                    .andExpect(status().isNoContent());

            verify(urlScanRepository).findByIdAndUserId(1L, 1L);
            verify(urlScanRepository).delete(scan);
        }
    }

    @Test
    void deleteScan_NotFound() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(urlScanRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(delete("/api/scans/999"))
                    .andExpect(status().isNotFound());

            verify(urlScanRepository).findByIdAndUserId(999L, 1L);
            verify(urlScanRepository, never()).delete(any());
        }
    }

    @Test
    void deleteScan_WrongUser() throws Exception {
        // Given - User 2 trying to delete scan belonging to user 1
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            when(urlScanRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(delete("/api/scans/1"))
                    .andExpect(status().isNotFound());

            verify(urlScanRepository).findByIdAndUserId(1L, 2L);
            verify(urlScanRepository, never()).delete(any());
        }
    }

    private UrlScan createUrlScan(Long id, String url, Long userId) {
        UrlScan scan = new UrlScan(url, userId);
        scan.setId(id);
        return scan;
    }
}