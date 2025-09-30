package com.geeknarrator.urlscanner.worker;

import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import com.geeknarrator.urlscanner.service.UrlScanIoClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component
public class UrlScanWorker {

    private static final Logger logger = LoggerFactory.getLogger(UrlScanWorker.class);

    private final UrlScanRepository urlScanRepository;
    private final UrlScanIoClient urlScanIoClient;
    private final MeterRegistry meterRegistry;

    @Value("${worker.submission.batch-size:100}")
    private int submissionBatchSize;

    @Value("${worker.result.batch-size:100}")
    private int resultBatchSize;

    @Value("${worker.fairness.per-user-batch-size:5}")
    private int perUserBatchSize;

    public UrlScanWorker(UrlScanRepository urlScanRepository, UrlScanIoClient urlScanIoClient, MeterRegistry meterRegistry) {
        this.urlScanRepository = urlScanRepository;
        this.urlScanIoClient = urlScanIoClient;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${worker.submission.delay-ms:10000}")
    @Transactional
    public void processSubmittedScans() {
        runFairnessWorker(UrlScan.ScanStatus.SUBMITTED, submissionBatchSize, this::processScan);
    }

    @Scheduled(fixedDelayString = "${worker.result.delay-ms:15000}")
    @Transactional
    public void checkProcessingScans() {
        runFairnessWorker(UrlScan.ScanStatus.PROCESSING, resultBatchSize, this::checkScanResult);
    }

    private void runFairnessWorker(UrlScan.ScanStatus status, int maxBatchSize, Consumer<UrlScan> processor) {
        logger.info("Running fairness worker for status: {}", status);
        int processedCount = 0;

        // --- Phase 1: Fairness Pass (Round-Robin per user) ---
        List<Long> userIds = urlScanRepository.findDistinctUserIdsWithStatus(status);
        if (!userIds.isEmpty()) {
            logger.info("Fairness pass: Found {} users with pending scans.", userIds.size());
            for (Long userId : userIds) {
                if (processedCount >= maxBatchSize) {
                    logger.info("Batch limit reached during fairness pass. Deferring remaining users.");
                    break;
                }
                Pageable perUserPageable = PageRequest.of(0, perUserBatchSize);
                Page<UrlScan> userScans = urlScanRepository.findAndLockByUserIdAndStatus(userId, status, perUserPageable);
                for (UrlScan scan : userScans) {
                    processor.accept(scan);
                    processedCount++;
                }
            }
        }

        // --- Phase 2: Efficiency Pass (Bulk processing for remaining capacity) ---
        int remainingCapacity = maxBatchSize - processedCount;
        if (remainingCapacity > 0) {
            logger.info("Efficiency pass: Fetching up to {} more scans.", remainingCapacity);
            Pageable bulkPageable = PageRequest.of(0, remainingCapacity);
            Page<UrlScan> bulkScans = urlScanRepository.findAndLockByStatus(status, bulkPageable);
            if (!bulkScans.isEmpty()) {
                logger.info("Found and locked {} additional scans in efficiency pass.", bulkScans.getNumberOfElements());
                for (UrlScan scan : bulkScans) {
                    processor.accept(scan);
                }
            }
        }
        logger.info("Finished worker run for status: {}", status);
    }

    private void processScan(UrlScan scan) {
        try {
            Optional<String> externalScanIdOpt = urlScanIoClient.submitScan(scan.getUrl());
            if (externalScanIdOpt.isPresent()) {
                scan.setExternalScanId(externalScanIdOpt.get());
                scan.setStatus(UrlScan.ScanStatus.PROCESSING);
                logger.info("Scan ID: {} successfully submitted. External ID: {}", scan.getId(), externalScanIdOpt.get());
            } else {
                handleFailure(scan, "submission_error", "Failed to submit scan to urlscan.io");
            }
        } catch (Exception e) {
            handleFailure(scan, "submission_error", "An unexpected error occurred while submitting scan: " + e.getMessage(), e);
        }
    }

    private void checkScanResult(UrlScan scan) {
        if (scan.getExternalScanId() == null || scan.getExternalScanId().isEmpty()) {
            handleFailure(scan, "invalid_state", "Scan is in PROCESSING state but has no external scan ID");
            return;
        }
        try {
            Optional<String> resultOpt = urlScanIoClient.getScanResult(scan.getExternalScanId());
            if (resultOpt.isPresent()) {
                scan.setResult(resultOpt.get());
                scan.setStatus(UrlScan.ScanStatus.DONE);
                meterRegistry.counter("scans.completed").increment();
                logger.info("Successfully fetched result for scan ID: {}. Status set to DONE.", scan.getId());
            } else {
                logger.info("Result for scan ID: {} not yet available.", scan.getId());
            }
        } catch (Exception e) {
            handleFailure(scan, "result_error", "An unexpected error occurred while checking result: " + e.getMessage(), e);
        }
    }

    private void handleFailure(UrlScan scan, String reasonCode, String errorMessage, Exception... e) {
        meterRegistry.counter("scans.failed", "reason", reasonCode).increment();
        scan.setStatus(UrlScan.ScanStatus.FAILED);
        scan.setFailureReason(errorMessage);
        if (e.length > 0) {
            logger.error("Scan ID: {} failed. Reason: {}. Details: {}", scan.getId(), errorMessage, e[0].getMessage());
        } else {
            logger.error("Scan ID: {} failed. Reason: {}.", scan.getId(), errorMessage);
        }
    }
}
