package com.uthman.VaultApi.ai;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiPlannerService aiPlannerService;

    public AiController(AiPlannerService aiPlannerService) {
        this.aiPlannerService = aiPlannerService;
    }

    @PostMapping("/plan")
    public ResponseEntity<AiPlan> plan(@Valid @RequestBody AiPlannerRequest request) {
        return ResponseEntity.ok(aiPlannerService.generatePlan(request.getPrompt()));
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(Map.of("reply", aiPlannerService.chatReply(request.getMessages())));
    }

    @PostMapping("/plan-from-chat")
    public ResponseEntity<AiPlan> planFromChat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(aiPlannerService.planFromConversation(request.getMessages()));
    }
}
