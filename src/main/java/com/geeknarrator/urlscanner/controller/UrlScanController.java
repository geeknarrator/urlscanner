package com.geeknarrator.urlscanner.controller;

import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import com.geeknarrator.urlscanner.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/scans")
public class UrlScanController {

    @Autowired
    private UrlScanRepository urlScanRepository;

    @Value("${urlscan.cache.ttl.hours:24}")
    private int cacheTtlHours;

    @PostMapping
    public ResponseEntity<UrlScan> createScan(@Valid @RequestBody CreateScanRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime since = LocalDateTime.now().minusHours(cacheTtlHours);

        // --- Step 1: User-level Deduplication ---
        // Check if the current user already has a recent scan for this exact URL.
        Optional<UrlScan> userExistingScan = urlScanRepository.findFirstByUserIdAndUrlAndCreatedAtAfterOrderByCreatedAtDesc(
                userId,
                request.getUrl(),
                since
        );

        if (userExistingScan.isPresent()) {
            // If the user already has a recent scan, just return it to prevent duplicates.
            return ResponseEntity.ok(userExistingScan.get());
        }

        // --- Step 2: Global Cache Check ---
        // Check for a recent, completed scan from *any* user to use as a cache.
        Optional<UrlScan> globalCachedScan = urlScanRepository.findFirstByUrlAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                request.getUrl(),
                UrlScan.ScanStatus.DONE,
                since
        );

        if (globalCachedScan.isPresent()) {
            // Cache hit: Create a new scan record for this user with the cached result.
            UrlScan scanFromCache = globalCachedScan.get();
            UrlScan newScan = new UrlScan(request.getUrl(), userId);
            newScan.setStatus(UrlScan.ScanStatus.DONE); // Instantly done
            newScan.setResult(scanFromCache.getResult()); // Copy the result
            newScan.setExternalScanId(scanFromCache.getExternalScanId()); // Copy the external ID
            UrlScan savedScan = urlScanRepository.save(newScan);
            return ResponseEntity.ok(savedScan);
        }

        // --- Step 3: New Submission ---
        // Cache miss: Create a new scan to be processed by the worker.
        UrlScan newScan = new UrlScan(request.getUrl(), userId);
        UrlScan savedScan = urlScanRepository.save(newScan);
        return ResponseEntity.ok(savedScan);
    }

    @GetMapping
    public ResponseEntity<Page<UrlScan>> getAllScans(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<UrlScan> scans = urlScanRepository.findByUserId(userId, pageable);
        return ResponseEntity.ok(scans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UrlScan> getScanById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Optional<UrlScan> scan = urlScanRepository.findByIdAndUserId(id, userId);
        return scan.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScan(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Optional<UrlScan> scan = urlScanRepository.findByIdAndUserId(id, userId);

        if (scan.isPresent()) {
            urlScanRepository.delete(scan.get());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public static class CreateScanRequest {
        @NotBlank(message = "URL is required")
        @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}