package com.uthman.VaultApi;

import com.uthman.VaultApi.ai.AiPlannerService;
import com.uthman.VaultApi.exception.AiUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiPlannerServiceTests {

    @Autowired private AiPlannerService aiPlannerService;

    @Test
    void planWithoutApiKeyThrowsAiUnavailable() {
        AiUnavailableException ex = assertThrows(
                AiUnavailableException.class,
                () -> aiPlannerService.generatePlan("I want to run a morning run every day"));
        assertTrue(ex.getMessage().contains("AI_API_KEY"));
    }
}
