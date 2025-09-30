package com.geeknarrator.urlscanner.repository;

import com.geeknarrator.urlscanner.entity.UrlScan;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlScanRepository extends JpaRepository<UrlScan, Long> {

    Page<UrlScan> findByUserId(Long userId, Pageable pageable);

    Optional<UrlScan> findByIdAndUserId(Long id, Long userId);

    Optional<UrlScan> findFirstByUserIdAndUrlAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, String url, LocalDateTime createdAt);

    Optional<UrlScan> findFirstByUrlAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(String url, UrlScan.ScanStatus status, LocalDateTime createdAt);

    long countByStatus(UrlScan.ScanStatus status);

    Optional<UrlScan> findByExternalScanId(String externalScanId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"),
        @QueryHint(name = "jakarta.persistence.lock.scope", value = "SKIP_LOCKED")
    })
    Page<UrlScan> findAndLockByStatus(UrlScan.ScanStatus status, Pageable pageable);

    @Query("SELECT DISTINCT u.userId FROM UrlScan u WHERE u.status = :status")
    List<Long> findDistinctUserIdsWithStatus(UrlScan.ScanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"),
        @QueryHint(name = "jakarta.persistence.lock.scope", value = "SKIP_LOCKED")
    })
    Page<UrlScan> findAndLockByUserIdAndStatus(Long userId, UrlScan.ScanStatus status, Pageable pageable);

    List<UrlScan> findByStatus(UrlScan.ScanStatus status);
}
