package com.uthman.VaultApi.ai;

import java.util.List;

public record AiPlan(
        String title,
        String description,
        String category,
        List<String> tags,
        List<String> milestones) {
}
