package com.geeknarrator.urlscanner.repository;

import com.geeknarrator.urlscanner.entity.UrlScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlScanRepository extends JpaRepository<UrlScan, Long> {
    
    List<UrlScan> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<UrlScan> findByUserIdAndStatus(Long userId, UrlScan.ScanStatus status);
    
    Optional<UrlScan> findByIdAndUserId(Long id, Long userId);
    
    @Query("SELECT u FROM UrlScan u WHERE u.url = :url AND u.status = 'DONE' AND u.createdAt > :since")
    Optional<UrlScan> findRecentCompletedScanByUrl(String url, LocalDateTime since);
    
    List<UrlScan> findByStatus(UrlScan.ScanStatus status);
    
    Optional<UrlScan> findByExternalScanId(String externalScanId);
}