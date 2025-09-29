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

    /**
     * Finds the most recent scan for a specific URL submitted by a specific user after a given time.
     * This is used for user-level deduplication to prevent a user from submitting the same URL multiple times.
     */
    Optional<UrlScan> findFirstByUserIdAndUrlAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, String url, LocalDateTime createdAt);

    /**
     * Finds the most recent completed scan for a given URL after a specific time, across all users.
     * This is used for global caching to avoid re-scanning the same URL.
     */
    Optional<UrlScan> findFirstByUrlAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(String url, UrlScan.ScanStatus status, LocalDateTime createdAt);

    List<UrlScan> findByStatus(UrlScan.ScanStatus status);

    Optional<UrlScan> findByExternalScanId(String externalScanId);
}
