package com.uthman.VaultApi;

import com.uthman.VaultApi.ai.AiPlannerService;
import com.uthman.VaultApi.ai.ChatMessage;
import com.uthman.VaultApi.exception.AiUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

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

    @Test
    void chatReplyWithoutApiKeyThrowsAiUnavailable() {
        AiUnavailableException ex = assertThrows(
                AiUnavailableException.class,
                () -> aiPlannerService.chatReply(List.of(new ChatMessage("user", "I want to get fit"))));
        assertTrue(ex.getMessage().contains("AI_API_KEY"));
    }

    @Test
    void planFromConversationWithoutApiKeyThrowsAiUnavailable() {
        AiUnavailableException ex = assertThrows(
                AiUnavailableException.class,
                () -> aiPlannerService.planFromConversation(
                        List.of(new ChatMessage("user", "I want to read 12 books this year"))));
        assertTrue(ex.getMessage().contains("AI_API_KEY"));
    }

    @Test
    void chatReplyWithEmptyMessagesThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> aiPlannerService.chatReply(List.of()));
    }

    @Test
    void planFromConversationWithEmptyMessagesThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> aiPlannerService.planFromConversation(null));
    }
}
