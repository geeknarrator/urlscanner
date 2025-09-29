package com.geeknarrator.urlscanner.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "url_scans")
public class UrlScan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 2048)
    private String url;
    
    @Column(name = "scan_status")
    @Enumerated(EnumType.STRING)
    private ScanStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(columnDefinition = "TEXT")
    private String result;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "external_scan_id")
    private String externalScanId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;
    
    public UrlScan() {}
    
    public UrlScan(String url, Long userId) {
        this.url = url;
        this.userId = userId;
        this.status = ScanStatus.SUBMITTED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public ScanStatus getStatus() {
        return status;
    }
    
    public void setStatus(ScanStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getExternalScanId() {
        return externalScanId;
    }
    
    public void setExternalScanId(String externalScanId) {
        this.externalScanId = externalScanId;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum ScanStatus {
        SUBMITTED,
        PROCESSING,
        DONE,
        FAILED
    }
}