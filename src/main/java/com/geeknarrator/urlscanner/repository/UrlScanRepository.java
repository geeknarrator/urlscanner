package com.geeknarrator.urlscanner.repository;

import com.geeknarrator.urlscanner.entity.UrlScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlScanRepository extends JpaRepository<UrlScan, Long> {
    
    List<UrlScan> findByStatus(UrlScan.ScanStatus status);
    
    @Query("SELECT u FROM UrlScan u ORDER BY u.createdAt DESC")
    List<UrlScan> findAllOrderByCreatedAtDesc();
}