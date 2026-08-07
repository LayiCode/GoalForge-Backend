package com.uthman.VaultApi.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uthman.VaultApi.exception.AiUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiPlannerService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final String apiKey;

    public AiPlannerService(@Value("${AI_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public AiPlan generatePlan(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiUnavailableException(
                    "AI planning is not configured yet. Add AI_API_KEY in the Render dashboard to enable it.");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Tell us a bit about the goal you want to plan");
        }

        String system = """
                You are a goal-planning assistant for GoalForge. Turn the user's idea into a clear,
                actionable goal plan. Respond ONLY with valid JSON (no markdown fences), exactly in this shape:
                {"title": string, "description": string, "category": one of [Career, Health, Finance, Personal, Education, Travel],
                "tags": string[], "milestones": string[]}
                Give 3 to 6 specific, measurable milestones. Keep the title under 60 characters and the description under 200 characters.
                """;

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AiUnavailableException(
                        "AI provider returned an error (" + response.statusCode() + "). Please try again shortly.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            return parsePlan(content);
        } catch (IOException e) {
            throw new AiUnavailableException("Could not reach the AI provider. Please try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Could not reach the AI provider. Please try again.");
        }
    }

    private AiPlan parsePlan(String content) {
        try {
            String trimmed = content.trim();
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            String json = (start >= 0 && end > start) ? trimmed.substring(start, end + 1) : trimmed;

            JsonNode node = objectMapper.readTree(json);
            String title = node.path("title").asText("");
            String description = node.path("description").asText("");
            String category = node.path("category").asText("");
            List<String> tags = new ArrayList<>();
            node.path("tags").forEach(tag -> tags.add(tag.asText()));
            List<String> milestones = new ArrayList<>();
            node.path("milestones").forEach(milestone -> milestones.add(milestone.asText()));

            if (title.isBlank() || milestones.isEmpty()) {
                throw new AiUnavailableException("AI returned an unreadable plan. Please try again.");
            }
            return new AiPlan(title, description, category, tags, milestones);
        } catch (IOException e) {
            throw new AiUnavailableException("AI returned an unreadable plan. Please try again.");
        }
    }
}
