package com.uthman.VaultApi.goal;

import com.uthman.VaultApi.exception.ForbiddenException;
import com.uthman.VaultApi.exception.NotFoundException;
import com.uthman.VaultApi.note.Note;
import com.uthman.VaultApi.note.NoteRepository;
import com.uthman.VaultApi.resource.Resource;
import com.uthman.VaultApi.resource.ResourceRepository;
import com.uthman.VaultApi.user.User;
import com.uthman.VaultApi.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final MilestoneRepository milestoneRepository;
    private final NoteRepository noteRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public GoalService(GoalRepository goalRepository,
                       MilestoneRepository milestoneRepository,
                       NoteRepository noteRepository,
                       ResourceRepository resourceRepository,
                       UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.milestoneRepository = milestoneRepository;
        this.noteRepository = noteRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    // Get logged in user
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    // Get all goals with pagination, search and filters
    public Page<Goal> getMyGoals(int page, int size, String search,
                                 Goal.Status status, String category, String tag) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Long userId = getCurrentUser().getId();
        return goalRepository.search(userId, search, status, category, tag, pageable);
    }

    // Get single goal (owner only)
    public Goal getGoal(Long id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        if (!goal.getUser().getId().equals(getCurrentUser().getId())) {
            throw new ForbiddenException("You do not have access to this goal");
        }
        return goal;
    }

    // Get a public goal without authentication
    public Goal getPublicGoal(Long id) {
        return goalRepository.findByIdAndIsPublicTrue(id)
                .orElseThrow(() -> new NotFoundException("Goal not found or not shared"));
    }

    // Create goal
    public Goal createGoal(Goal goal) {
        goal.setUser(getCurrentUser());
        return goalRepository.save(goal);
    }

    // Update goal
    public Goal updateGoal(Long id, Goal updated) {
        Goal goal = getGoal(id);
        goal.setTitle(updated.getTitle());
        goal.setDescription(updated.getDescription());
        goal.setCategory(updated.getCategory());
        goal.setStatus(updated.getStatus());
        goal.setTargetDate(updated.getTargetDate());
        goal.setTags(updated.getTags());
        goal.setPublic(updated.isPublic());
        return goalRepository.save(goal);
    }

    // Delete goal
    public void deleteGoal(Long id) {
        goalRepository.delete(getGoal(id));
    }

    // Add milestone to goal
    public Milestone addMilestone(Long goalId, Milestone milestone) {
        milestone.setGoal(getGoal(goalId));
        return milestoneRepository.save(milestone);
    }

    // Update milestone (owner of parent goal only)
    public Milestone updateMilestone(Long goalId, Long milestoneId, Milestone updated) {
        getGoal(goalId);
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new NotFoundException("Milestone not found"));
        milestone.setTitle(updated.getTitle());
        milestone.setCompleted(updated.isCompleted());
        return milestoneRepository.save(milestone);
    }

    // Delete milestone (owner of parent goal only)
    public void deleteMilestone(Long goalId, Long milestoneId) {
        getGoal(goalId);
        milestoneRepository.deleteById(milestoneId);
    }

    // ----- Notes (journal) -----

    public List<Note> getNotes(Long goalId) {
        getGoal(goalId);
        return noteRepository.findByGoalIdOrderByCreatedAtDesc(goalId);
    }

    public Note addNote(Long goalId, Note note) {
        note.setGoal(getGoal(goalId));
        return noteRepository.save(note);
    }

    public Note updateNote(Long goalId, Long noteId, Note updated) {
        getGoal(goalId);
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
        note.setContent(updated.getContent());
        return noteRepository.save(note);
    }

    public void deleteNote(Long goalId, Long noteId) {
        getGoal(goalId);
        noteRepository.deleteById(noteId);
    }

    // ----- Resources -----

    public List<Resource> getResources(Long goalId) {
        getGoal(goalId);
        return resourceRepository.findByGoalIdOrderByIdAsc(goalId);
    }

    public Resource addResource(Long goalId, Resource resource) {
        resource.setGoal(getGoal(goalId));
        return resourceRepository.save(resource);
    }

    public Resource updateResource(Long goalId, Long resourceId, Resource updated) {
        getGoal(goalId);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        resource.setTitle(updated.getTitle());
        resource.setUrl(updated.getUrl());
        return resourceRepository.save(resource);
    }

    public void deleteResource(Long goalId, Long resourceId) {
        getGoal(goalId);
        resourceRepository.deleteById(resourceId);
    }

    // ----- Reminders -----

    public List<Goal> getReminders(int days) {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(Math.max(0, days));
        return goalRepository.findUpcoming(getCurrentUser().getId(), today, dueDate);
    }

    // ----- Analytics -----

    public Map<String, Object> getStats() {
        List<Goal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(getCurrentUser().getId());

        long total = goals.size();
        Map<String, Long> byStatus = goals.stream()
                .collect(Collectors.groupingBy(g -> g.getStatus().name(), Collectors.counting()));
        Map<String, Long> byCategory = goals.stream()
                .filter(g -> g.getCategory() != null && !g.getCategory().isBlank())
                .collect(Collectors.groupingBy(g -> g.getCategory(), Collectors.counting()));

        long completedMilestones = goals.stream()
                .flatMap(g -> g.getMilestones() == null ? java.util.stream.Stream.empty()
                        : g.getMilestones().stream())
                .filter(Milestone::isCompleted)
                .count();
        long totalMilestones = goals.stream()
                .filter(g -> g.getMilestones() != null)
                .mapToLong(g -> g.getMilestones().size())
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGoals", total);
        stats.put("totalMilestones", totalMilestones);
        stats.put("completedMilestones", completedMilestones);
        stats.put("milestoneCompletionRate",
                totalMilestones == 0 ? 0 : Math.round((completedMilestones * 100.0) / totalMilestones));
        stats.put("byStatus", byStatus);
        stats.put("byCategory", byCategory);
        stats.put("publicGoals", goals.stream().filter(Goal::isPublic).count());
        return stats;
    }
}
