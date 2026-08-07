package com.uthman.VaultApi.ai;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
