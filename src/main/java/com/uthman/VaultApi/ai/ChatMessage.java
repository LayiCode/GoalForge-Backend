package com.uthman.VaultApi.ai;

import jakarta.validation.constraints.NotBlank;

public record ChatMessage(
        @NotBlank(message = "Message role is required") String role,
        @NotBlank(message = "Message content is required") String content) {
}
