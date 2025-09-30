package com.geeknarrator.urlscanner.repository;

import com.geeknarrator.urlscanner.entity.UrlScan;
import com.geeknarrator.urlscanner.entity.User;
import com.geeknarrator.urlscanner.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UrlScanRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UrlScanRepository urlScanRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1, user2, user3;

    @BeforeEach
    void setUp() {
        // Clean up tables before each test
        urlScanRepository.deleteAll();
        userRepository.deleteAll();

        // Create and persist users
        user1 = userRepository.save(new User("user1@test.com", "password", "User", "One"));
        user2 = userRepository.save(new User("user2@test.com", "password", "User", "Two"));
        user3 = userRepository.save(new User("user3@test.com", "password", "User", "Three"));
    }

    @Test
    void whenFindDistinctUserIdsWithStatus_thenReturnDistinctUserIds() {
        // given
        urlScanRepository.save(new UrlScan("https://example.com", user1.getId()));
        urlScanRepository.save(new UrlScan("https://example.org", user1.getId()));
        UrlScan user2Scan = new UrlScan("https://example.net", user2.getId());
        user2Scan.setStatus(UrlScan.ScanStatus.PROCESSING);
        urlScanRepository.save(user2Scan);
        urlScanRepository.save(new UrlScan("https://example.io", user3.getId()));

        // when
        List<Long> userIds = urlScanRepository.findDistinctUserIdsWithStatus(UrlScan.ScanStatus.SUBMITTED);

        // then
        assertThat(userIds).hasSize(2).containsExactlyInAnyOrder(user1.getId(), user3.getId());
    }

    @Test
    void whenFindAndLockByUserIdAndStatus_thenReturnCorrectPageForUser() {
        // given
        urlScanRepository.save(new UrlScan("https://a.com", user1.getId()));
        urlScanRepository.save(new UrlScan("https://b.com", user1.getId()));
        urlScanRepository.save(new UrlScan("https://c.com", user1.getId()));
        urlScanRepository.save(new UrlScan("https://d.com", user2.getId()));

        // when
        Page<UrlScan> resultPage = urlScanRepository.findAndLockByUserIdAndStatus(user1.getId(), UrlScan.ScanStatus.SUBMITTED, PageRequest.of(0, 2));

        // then
        assertThat(resultPage.getTotalElements()).isEqualTo(3);
        assertThat(resultPage.getContent()).hasSize(2);
        assertThat(resultPage.getContent()).allMatch(scan -> scan.getUserId().equals(user1.getId()));
    }

    @Test
    void whenFindAndLockByStatus_thenReturnPagedResult() {
        // given
        urlScanRepository.save(new UrlScan("https://a.com", user1.getId()));
        urlScanRepository.save(new UrlScan("https://b.com", user1.getId()));
        urlScanRepository.save(new UrlScan("https://c.com", user2.getId()));

        // when
        Page<UrlScan> resultPage = urlScanRepository.findAndLockByStatus(UrlScan.ScanStatus.SUBMITTED, PageRequest.of(0, 2));

        // then
        assertThat(resultPage.getTotalElements()).isEqualTo(3);
        assertThat(resultPage.getContent()).hasSize(2);
    }
}
