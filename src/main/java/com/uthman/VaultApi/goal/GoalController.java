package com.uthman.VaultApi.goal;

import com.uthman.VaultApi.note.Note;
import com.uthman.VaultApi.resource.Resource;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    // Get all my goals with pagination, search and filters
    @GetMapping
    public ResponseEntity<Page<Goal>> getMyGoals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Goal.Status status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {
        return ResponseEntity.ok(goalService.getMyGoals(page, size, search, status, category, tag));
    }

    // Get single goal
    @GetMapping("/{id}")
    public ResponseEntity<Goal> getGoal(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getGoal(id));
    }

    // Get a public (shared) goal - no auth required
    @GetMapping("/public/{id}")
    public ResponseEntity<Goal> getPublicGoal(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getPublicGoal(id));
    }

    // Create goal
    @PostMapping
    public ResponseEntity<Goal> createGoal(@Valid @RequestBody Goal goal) {
        return ResponseEntity.ok(goalService.createGoal(goal));
    }

    // Update goal
    @PutMapping("/{id}")
    public ResponseEntity<Goal> updateGoal(@PathVariable Long id,
                                           @Valid @RequestBody Goal goal) {
        return ResponseEntity.ok(goalService.updateGoal(id, goal));
    }

    // Delete goal
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.ok("Goal deleted successfully");
    }

    // Add milestone
    @PostMapping("/{id}/milestones")
    public ResponseEntity<Milestone> addMilestone(@PathVariable Long id,
                                                  @Valid @RequestBody Milestone milestone) {
        return ResponseEntity.ok(goalService.addMilestone(id, milestone));
    }

    // Update milestone
    @PutMapping("/{id}/milestones/{mId}")
    public ResponseEntity<Milestone> updateMilestone(@PathVariable Long id,
                                                     @PathVariable Long mId,
                                                     @Valid @RequestBody Milestone milestone) {
        return ResponseEntity.ok(goalService.updateMilestone(id, mId, milestone));
    }

    // Delete milestone
    @DeleteMapping("/{id}/milestones/{mId}")
    public ResponseEntity<String> deleteMilestone(@PathVariable Long id,
                                                  @PathVariable Long mId) {
        goalService.deleteMilestone(id, mId);
        return ResponseEntity.ok("Milestone deleted successfully");
    }

    // ----- Notes (journal) -----

    @GetMapping("/{id}/notes")
    public ResponseEntity<List<Note>> getNotes(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getNotes(id));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<Note> addNote(@PathVariable Long id,
                                        @Valid @RequestBody Note note) {
        return ResponseEntity.ok(goalService.addNote(id, note));
    }

    @PutMapping("/{id}/notes/{nId}")
    public ResponseEntity<Note> updateNote(@PathVariable Long id,
                                           @PathVariable Long nId,
                                           @Valid @RequestBody Note note) {
        return ResponseEntity.ok(goalService.updateNote(id, nId, note));
    }

    @DeleteMapping("/{id}/notes/{nId}")
    public ResponseEntity<String> deleteNote(@PathVariable Long id,
                                             @PathVariable Long nId) {
        goalService.deleteNote(id, nId);
        return ResponseEntity.ok("Note deleted successfully");
    }

    // ----- Resources -----

    @GetMapping("/{id}/resources")
    public ResponseEntity<List<Resource>> getResources(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getResources(id));
    }

    @PostMapping("/{id}/resources")
    public ResponseEntity<Resource> addResource(@PathVariable Long id,
                                                @Valid @RequestBody Resource resource) {
        return ResponseEntity.ok(goalService.addResource(id, resource));
    }

    @PutMapping("/{id}/resources/{rId}")
    public ResponseEntity<Resource> updateResource(@PathVariable Long id,
                                                   @PathVariable Long rId,
                                                   @Valid @RequestBody Resource resource) {
        return ResponseEntity.ok(goalService.updateResource(id, rId, resource));
    }

    @DeleteMapping("/{id}/resources/{rId}")
    public ResponseEntity<String> deleteResource(@PathVariable Long id,
                                                 @PathVariable Long rId) {
        goalService.deleteResource(id, rId);
        return ResponseEntity.ok("Resource deleted successfully");
    }

    // ----- Reminders -----

    @GetMapping("/reminders")
    public ResponseEntity<List<Goal>> getReminders(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(goalService.getReminders(days));
    }

    // ----- Analytics -----

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(goalService.getStats());
    }
}
