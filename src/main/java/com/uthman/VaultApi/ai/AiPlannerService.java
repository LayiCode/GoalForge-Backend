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

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final int MAX_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 2500;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final String apiKey;
    private final String model;

    public AiPlannerService(
            @Value("${AI_API_KEY:}") String apiKey,
            @Value("${AI_MODEL:}") String model) {
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;
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
                actionable goal plan. Give 3 to 6 specific, measurable milestones. Keep the title under
                60 characters and the description under 200 characters. Respond only with the JSON shape
                required by the schema.
                """;

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", system))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "responseMimeType", "application/json",
                        "responseSchema", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "title", Map.of("type", "STRING"),
                                        "description", Map.of("type", "STRING"),
                                        "category", Map.of("type", "STRING",
                                                "enum", List.of("Career", "Health", "Finance", "Personal", "Education", "Travel")),
                                        "tags", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                                        "milestones", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
                                ),
                                "required", List.of("title", "description", "category", "tags", "milestones")
                        )
                )
        );

        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_BASE_URL + model + ":generateContent?key=" + apiKey))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            int attempt = 1;
            while (true) {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parsePlan(extractText(response.body()));
                }
                if (response.statusCode() == 429 && attempt < MAX_ATTEMPTS) {
                    attempt++;
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }
                throw new AiUnavailableException(extractProviderError(response));
            }
        } catch (IOException e) {
            throw new AiUnavailableException("Could not reach the AI provider. Please try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Could not reach the AI provider. Please try again.");
        }
    }

    private String extractText(String body) throws IOException {
        JsonNode candidates = objectMapper.readTree(body).path("candidates");
        if (!candidates.isArray() || candidates.size() == 0) {
            return "";
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        return (parts.isArray() && parts.size() > 0) ? parts.get(0).path("text").asText("") : "";
    }

    private String extractProviderError(HttpResponse<String> response) {
        try {
            JsonNode error = objectMapper.readTree(response.body()).path("error");
            String message = error.path("message").asText("");
            if (!message.isBlank()) {
                return "AI provider: " + message;
            }
        } catch (IOException ignored) {
            // fall through to the generic message
        }
        return "AI provider returned an error (" + response.statusCode() + "). Please try again shortly.";
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
