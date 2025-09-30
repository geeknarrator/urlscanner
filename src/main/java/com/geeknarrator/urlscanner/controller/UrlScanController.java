package com.geeknarrator.urlscanner.controller;

import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import com.geeknarrator.urlscanner.security.SecurityUtils;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    private final UrlScanRepository urlScanRepository;
    private final MeterRegistry meterRegistry;

    @Value("${urlscan.cache.ttl.hours:24}")
    private int cacheTtlHours;

    public UrlScanController(UrlScanRepository urlScanRepository, MeterRegistry meterRegistry) {
        this.urlScanRepository = urlScanRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping
    public ResponseEntity<UrlScan> createScan(@Valid @RequestBody CreateScanRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime since = LocalDateTime.now().minusHours(cacheTtlHours);

        // Step 1: User-level Deduplication
        Optional<UrlScan> userExistingScan = urlScanRepository.findFirstByUserIdAndUrlAndCreatedAtAfterOrderByCreatedAtDesc(
                userId,
                request.getUrl(),
                since
        );
        if (userExistingScan.isPresent()) {
            meterRegistry.counter("scans.cache.hit", "type", "user").increment();
            return ResponseEntity.ok(userExistingScan.get());
        }

        // Step 2: Global Cache Check
        Optional<UrlScan> globalCachedScan = urlScanRepository.findFirstByUrlAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                request.getUrl(),
                UrlScan.ScanStatus.DONE,
                since
        );
        if (globalCachedScan.isPresent()) {
            meterRegistry.counter("scans.cache.hit", "type", "global").increment();
            UrlScan scanFromCache = globalCachedScan.get();
            UrlScan newScan = new UrlScan(request.getUrl(), userId);
            newScan.setStatus(UrlScan.ScanStatus.DONE);
            newScan.setResult(scanFromCache.getResult());
            newScan.setExternalScanId(scanFromCache.getExternalScanId());
            UrlScan savedScan = urlScanRepository.save(newScan);
            return ResponseEntity.ok(savedScan);
        }

        // Step 3: New Submission (Cache Miss)
        meterRegistry.counter("scans.submitted", "type", "new").increment();
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
