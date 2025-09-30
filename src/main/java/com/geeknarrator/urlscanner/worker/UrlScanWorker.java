package com.geeknarrator.urlscanner.worker;

import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import com.geeknarrator.urlscanner.service.UrlScanIoClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class UrlScanWorker {

    private static final Logger logger = LoggerFactory.getLogger(UrlScanWorker.class);

    private final UrlScanRepository urlScanRepository;
    private final UrlScanIoClient urlScanIoClient;
    private final MeterRegistry meterRegistry;

    public UrlScanWorker(UrlScanRepository urlScanRepository, UrlScanIoClient urlScanIoClient, MeterRegistry meterRegistry) {
        this.urlScanRepository = urlScanRepository;
        this.urlScanIoClient = urlScanIoClient;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processSubmittedScans() {
        logger.info("Running worker to process SUBMITTED scans...");
        List<UrlScan> submittedScans = urlScanRepository.findByStatus(UrlScan.ScanStatus.SUBMITTED);

        if (submittedScans.isEmpty()) {
            return;
        }

        logger.info("Found {} SUBMITTED scans to process.", submittedScans.size());
        for (UrlScan scan : submittedScans) {
            processScan(scan);
        }
        logger.info("Finished processing batch of SUBMITTED scans.");
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

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void checkProcessingScans() {
        logger.info("Running worker to check PROCESSING scans...");
        List<UrlScan> processingScans = urlScanRepository.findByStatus(UrlScan.ScanStatus.PROCESSING);

        if (processingScans.isEmpty()) {
            return;
        }

        logger.info("Found {} PROCESSING scans to check.", processingScans.size());
        for (UrlScan scan : processingScans) {
            checkScanResult(scan);
        }
        logger.info("Finished checking batch of PROCESSING scans.");
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
            handleFailure(scan, "result_error", "An unexpected error occurred while checking result for scan: " + e.getMessage(), e);
        }
    }

    private void handleFailure(UrlScan scan, String reasonCode, String errorMessage, Exception... e) {
        meterRegistry.counter("scans.failed", "reason", reasonCode).increment();
        scan.setStatus(UrlScan.ScanStatus.FAILED);
        scan.setFailureReason(errorMessage);
        if (e.length > 0) {
            logger.error("Scan ID: {} failed. Reason: {}.", scan.getId(), errorMessage, e[0]);
        } else {
            logger.error("Scan ID: {} failed. Reason: {}.", scan.getId(), errorMessage);
        }
    }
}
