package com.telehealth.anonymization.job;

import com.telehealth.anonymization.repository.TokenNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final TokenNodeRepository tokenNodeRepository;

    @Scheduled(fixedRate = 600000)      // every 10 minutes
    public void cleanupExpiredTokens() {
        tokenNodeRepository.deleteAllExpiredTokens(LocalDateTime.now());
        System.out.println(LocalDateTime.now() + " | Neo4j: Background removal of expired pseudonyms and their relationships has been performed.");
    }
}
