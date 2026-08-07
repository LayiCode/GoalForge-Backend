package com.uthman.VaultApi.ai;

import jakarta.validation.constraints.NotBlank;

public class AiPlannerRequest {

    @NotBlank(message = "Tell us a bit about the goal you want to plan")
    private String prompt;

    public String getPrompt() { return prompt; }

    public void setPrompt(String prompt) { this.prompt = prompt; }
}
