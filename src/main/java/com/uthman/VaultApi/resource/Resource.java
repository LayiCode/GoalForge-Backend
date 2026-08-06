package com.uthman.VaultApi.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uthman.VaultApi.goal.Goal;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "URL is required")
    @Column(nullable = false)
    private String url;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public Goal getGoal() { return goal; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setUrl(String url) { this.url = url; }
    public void setGoal(Goal goal) { this.goal = goal; }
}
