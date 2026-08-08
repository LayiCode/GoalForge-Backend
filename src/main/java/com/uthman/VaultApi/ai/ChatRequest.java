package com.uthman.VaultApi.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ChatRequest {

    @NotEmpty(message = "Conversation must not be empty")
    @Valid
    private List<ChatMessage> messages;

    public List<ChatMessage> getMessages() { return messages; }

    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
}
