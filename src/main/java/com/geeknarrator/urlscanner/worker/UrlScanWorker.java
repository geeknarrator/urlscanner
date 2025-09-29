package com.geeknarrator.urlscanner.worker;

import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import com.geeknarrator.urlscanner.service.UrlScanIoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class UrlScanWorker {

    private static final Logger logger = LoggerFactory.getLogger(UrlScanWorker.class);

    @Autowired
    private UrlScanRepository urlScanRepository;

    @Autowired
    private UrlScanIoClient urlScanIoClient;

    /**
     * Periodically fetches submitted scans and sends them to urlscan.io.
     */
    @Scheduled(fixedDelay = 10000) // Run every 10 seconds
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
            logger.info("Processing scan ID: {} for URL: {}", scan.getId(), scan.getUrl());
            Optional<String> externalScanIdOpt = urlScanIoClient.submitScan(scan.getUrl());

            if (externalScanIdOpt.isPresent()) {
                scan.setExternalScanId(externalScanIdOpt.get());
                scan.setStatus(UrlScan.ScanStatus.PROCESSING);
                logger.info("Scan ID: {} successfully submitted. External ID: {}", scan.getId(), externalScanIdOpt.get());
            } else {
                scan.setStatus(UrlScan.ScanStatus.FAILED);
                logger.error("Failed to submit scan ID: {} to urlscan.io", scan.getId());
            }
        } catch (Exception e) {
            scan.setStatus(UrlScan.ScanStatus.FAILED);
            logger.error("An unexpected error occurred while processing scan ID: {}", scan.getId(), e);
        }
        // The transaction will commit the changes to the scan object
    }

    /**
     * Periodically checks for the results of scans that are in progress.
     */
    @Scheduled(fixedDelay = 15000) // Run every 15 seconds
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
            logger.error("Scan ID: {} is in PROCESSING state but has no external scan ID. Marking as FAILED.", scan.getId());
            scan.setStatus(UrlScan.ScanStatus.FAILED);
            return;
        }

        try {
            logger.info("Checking result for scan ID: {} (External ID: {})", scan.getId(), scan.getExternalScanId());
            Optional<String> resultOpt = urlScanIoClient.getScanResult(scan.getExternalScanId());

            if (resultOpt.isPresent()) {
                scan.setResult(resultOpt.get());
                scan.setStatus(UrlScan.ScanStatus.DONE);
                logger.info("Successfully fetched result for scan ID: {}. Status set to DONE.", scan.getId());
            } else {
                // Result is not ready yet, do nothing and wait for the next worker run.
                logger.info("Result for scan ID: {} not yet available.", scan.getId());
            }
        } catch (Exception e) {
            scan.setStatus(UrlScan.ScanStatus.FAILED);
            logger.error("An unexpected error occurred while checking result for scan ID: {}", scan.getId(), e);
        }
        // The transaction will commit the changes to the scan object
    }
}
