package com.uthman.VaultApi.goal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uthman.VaultApi.note.Note;
import com.uthman.VaultApi.resource.Resource;
import com.uthman.VaultApi.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    private String description;

    private String category;

    @Enumerated(EnumType.STRING)
    private Status status = Status.IN_PROGRESS;

    @FutureOrPresent(message = "Target date must be today or in the future")
    private LocalDate targetDate;

    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonProperty("isPublic")
    private boolean isPublic = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "goal_tags", joinColumns = @JoinColumn(name = "goal_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @JsonIgnoreProperties({"password", "goals"})
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Milestone> milestones;

    @JsonIgnore
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Note> notes;

    @JsonIgnore
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Resource> resources;

    public enum Status {
        IN_PROGRESS, COMPLETED, ABANDONED
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public Status getStatus() { return status; }
    public LocalDate getTargetDate() { return targetDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isPublic() { return isPublic; }
    public List<String> getTags() { return tags; }
    public User getUser() { return user; }
    public List<Milestone> getMilestones() { return milestones; }
    public List<Note> getNotes() { return notes; }
    public List<Resource> getResources() { return resources; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setStatus(Status status) { this.status = status; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setUser(User user) { this.user = user; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }
    public void setNotes(List<Note> notes) { this.notes = notes; }
    public void setResources(List<Resource> resources) { this.resources = resources; }
}