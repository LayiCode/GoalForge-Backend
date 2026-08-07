package com.uthman.VaultApi.goal;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CreateGoalRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String category;

    private Goal.Status status;

    @FutureOrPresent(message = "Target date must be today or in the future")
    private LocalDate targetDate;

    private List<String> tags = new ArrayList<>();

    @JsonProperty("isPublic")
    private boolean isPublic = false;

    private List<String> milestones = new ArrayList<>();

    // Getters and Setters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public Goal.Status getStatus() { return status; }
    public LocalDate getTargetDate() { return targetDate; }
    public List<String> getTags() { return tags; }
    public boolean isPublic() { return isPublic; }
    public List<String> getMilestones() { return milestones; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setStatus(Goal.Status status) { this.status = status; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setMilestones(List<String> milestones) { this.milestones = milestones; }
}
