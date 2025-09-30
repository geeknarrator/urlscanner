package com.geeknarrator.urlscanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class UrlScanIoClient {

    private static final Logger logger = LoggerFactory.getLogger(UrlScanIoClient.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${urlscan.api.key}")
    private String urlscanApiKey;

    @Value("${urlscan.api.url:https://urlscan.io/api/v1}")
    private String urlscanApiBaseUrl;

    @Value("${urlscan.client.max-retries:3}")
    private int maxRetries;

    @Value("${urlscan.client.retry-initial-delay-ms:5000}")
    private long retryInitialDelayMs;

    public UrlScanIoClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    public Optional<String> submitScan(String url) {
        String submitUrl = urlscanApiBaseUrl + "/scan/";
        HttpHeaders headers = createApiHeaders();

        Map<String, String> requestBody = Collections.singletonMap("url", url);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        long currentDelay = retryInitialDelayMs;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("Attempt {} to submit scan for URL: {}", attempt, url);
                ResponseEntity<Map> response = restTemplate.exchange(
                        submitUrl,
                        HttpMethod.POST,
                        requestEntity,
                        Map.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String uuid = (String) response.getBody().get("uuid");
                    logger.info("Scan submitted successfully for URL: {}, UUID: {}", url, uuid);
                    return Optional.ofNullable(uuid);
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    if (attempt < maxRetries) {
                        logger.warn("Rate limit hit for URL: {}. Retrying in {}ms (Attempt {}/{})", url, currentDelay, attempt, maxRetries);
                        try {
                            Thread.sleep(currentDelay);
                            currentDelay *= 2; // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            logger.error("Thread interrupted during retry delay.", ie);
                            return Optional.empty();
                        }
                    } else {
                        logger.error("Max retries reached for URL: {} due to rate limiting.", url);
                    }
                } else {
                    logger.error("HTTP client error submitting scan for URL: {}. Status: {}. Body: {}", url, e.getStatusCode(), e.getResponseBodyAsString(), e);
                    return Optional.empty(); // Don't retry on other client errors
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred while submitting scan for URL: {}", url, e);
                return Optional.empty(); // Don't retry on other exceptions
            }
        }
        return Optional.empty();
    }

    public Optional<String> getScanResult(String externalScanId) {
        String resultUrl = urlscanApiBaseUrl + "/result/" + externalScanId + "/";
        HttpEntity<Void> requestEntity = new HttpEntity<>(createApiHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    resultUrl,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(objectMapper.writeValueAsString(response.getBody()));
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.info("Scan result for {} not yet available (404).", externalScanId);
                return Optional.empty();
            }
            logger.error("HTTP client error fetching result for scan ID: {}. Status: {}. Body: {}", externalScanId, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing scan result for ID: {}", externalScanId, e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while fetching result for scan ID: {}", externalScanId, e);
        }

        return Optional.empty();
    }

    private HttpHeaders createApiHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("API-Key", urlscanApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
