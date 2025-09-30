package com.geeknarrator.urlscanner.config;

import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.repository.UrlScanRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    /**
     * Creates a custom gauge metric to monitor the number of scans that are
     * in the SUBMITTED state and waiting to be processed by the worker.
     *
     * @param meterRegistry The Micrometer MeterRegistry.
     * @param urlScanRepository The repository to query for the count.
     * @return The configured Gauge.
     */
    @Bean
    public Gauge submittedScansGauge(MeterRegistry meterRegistry, UrlScanRepository urlScanRepository) {
        return Gauge.builder("scans.pending", () -> urlScanRepository.countByStatus(UrlScan.ScanStatus.SUBMITTED))
                .description("The number of scans currently waiting to be processed")
                .register(meterRegistry);
    }
}
