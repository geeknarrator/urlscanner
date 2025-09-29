package com.geeknarrator.urlscanner.controller;

import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/scans")
public class UrlScanController {
    
    @Autowired
    private UrlScanRepository urlScanRepository;
    
    @PostMapping
    public ResponseEntity<UrlScan> createScan(@RequestBody CreateScanRequest request) {
        UrlScan scan = new UrlScan(request.getUrl());
        UrlScan savedScan = urlScanRepository.save(scan);
        return ResponseEntity.ok(savedScan);
    }
    
    @GetMapping
    public ResponseEntity<List<UrlScan>> getAllScans() {
        List<UrlScan> scans = urlScanRepository.findAllOrderByCreatedAtDesc();
        return ResponseEntity.ok(scans);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UrlScan> getScanById(@PathVariable Long id) {
        Optional<UrlScan> scan = urlScanRepository.findById(id);
        return scan.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<UrlScan>> getScansByStatus(@PathVariable String status) {
        try {
            UrlScan.ScanStatus scanStatus = UrlScan.ScanStatus.valueOf(status.toUpperCase());
            List<UrlScan> scans = urlScanRepository.findByStatus(scanStatus);
            return ResponseEntity.ok(scans);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    public static class CreateScanRequest {
        private String url;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
    }
}