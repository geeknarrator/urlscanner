package com.geeknarrator.urlscanner.repository;

import com.geeknarrator.urlscanner.entity.UrlScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlScanRepository extends JpaRepository<UrlScan, Long> {

    Page<UrlScan> findByUserId(Long userId, Pageable pageable);

    List<UrlScan> findByUserIdAndStatus(Long userId, UrlScan.ScanStatus status);

    Optional<UrlScan> findByIdAndUserId(Long id, Long userId);

    Optional<UrlScan> findFirstByUserIdAndUrlAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, String url, LocalDateTime createdAt);

    Optional<UrlScan> findFirstByUrlAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(String url, UrlScan.ScanStatus status, LocalDateTime createdAt);

    List<UrlScan> findByStatus(UrlScan.ScanStatus status);

    long countByStatus(UrlScan.ScanStatus status);

    Optional<UrlScan> findByExternalScanId(String externalScanId);
}
