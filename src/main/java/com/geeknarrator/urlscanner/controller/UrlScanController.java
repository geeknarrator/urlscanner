package com.geeknarrator.urlscanner.controller;

import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import com.geeknarrator.urlscanner.security.SecurityUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Scans", description = "Endpoints for managing URL scans")
@SecurityRequirement(name = "bearerAuth")
public class UrlScanController {

    private final UrlScanRepository urlScanRepository;
    private final MeterRegistry meterRegistry;

    @Value("${urlscan.cache.ttl.hours:24}")
    private int cacheTtlHours;

    public UrlScanController(UrlScanRepository urlScanRepository, MeterRegistry meterRegistry) {
        this.urlScanRepository = urlScanRepository;
        this.meterRegistry = meterRegistry;
    }

    @Operation(summary = "Submit a URL for scanning", description = "Submits a new URL for scanning. If a recent scan for the same URL exists, it may return a cached result.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scan submitted, or cached result returned"),
            @ApiResponse(responseCode = "400", description = "Invalid URL format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
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

    @Operation(summary = "List all user scans", description = "Returns a paginated list of all scans submitted by the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of scans"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Page<UrlScan>> getAllScans(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<UrlScan> scans = urlScanRepository.findByUserId(userId, pageable);
        return ResponseEntity.ok(scans);
    }

    @Operation(summary = "Get a specific scan by ID", description = "Returns the details of a single scan, including its status and results if available.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved scan details"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Scan not found or does not belong to the user")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UrlScan> getScanById(@Parameter(description = "ID of the scan to retrieve") @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Optional<UrlScan> scan = urlScanRepository.findByIdAndUserId(id, userId);
        return scan.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a scan by ID", description = "Deletes a specific scan and its associated results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Scan deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Scan not found or does not belong to the user")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScan(@Parameter(description = "ID of the scan to delete") @PathVariable Long id) {
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
