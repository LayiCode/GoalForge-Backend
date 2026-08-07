package com.uthman.VaultApi;

import com.uthman.VaultApi.auth.AuthService;
import com.uthman.VaultApi.auth.RegisterRequest;
import com.uthman.VaultApi.goal.CreateGoalRequest;
import com.uthman.VaultApi.goal.Goal;
import com.uthman.VaultApi.goal.GoalRepository;
import com.uthman.VaultApi.goal.GoalService;
import com.uthman.VaultApi.goal.Milestone;
import com.uthman.VaultApi.user.User;
import com.uthman.VaultApi.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GoalFlowTests {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private GoalRepository goalRepository;
    @Autowired private GoalService goalService;

    private static final String STRONG_PASSWORD = "Str0ng!Password";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User register(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setFullName("Test User");
        request.setPassword(STRONG_PASSWORD);
        authService.register(request);
        return userRepository.findByEmail(email).orElseThrow();
    }

    private void asUser(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private CreateGoalRequest request(String title, List<String> milestones) {
        CreateGoalRequest request = new CreateGoalRequest();
        request.setTitle(title);
        request.setDescription("Description for " + title);
        request.setCategory("Health");
        request.setTags(List.of("test", "milestones"));
        request.setPublic(true);
        request.setMilestones(milestones);
        return request;
    }

    @Test
    void createGoalWithMilestonesSavesGoalAndMilestones() {
        register("goal1@test.com");
        asUser("goal1@test.com");

        Goal saved = goalService.createGoal(request("Run a marathon", List.of("Couch to 5k", "5k in 40 minutes", "Run 10k")));

        assertNotNull(saved.getId());
        assertEquals("Run a marathon", saved.getTitle());
        assertEquals(3, saved.getMilestones().size());
        assertEquals(List.of("Couch to 5k", "5k in 40 minutes", "Run 10k"),
                saved.getMilestones().stream().map(Milestone::getTitle).toList());
        assertTrue(saved.getMilestones().stream().noneMatch(Milestone::isCompleted));
    }

    @Test
    void createGoalWithoutMilestonesSavesGoalOnly() {
        register("goal2@test.com");
        asUser("goal2@test.com");

        Goal saved = goalService.createGoal(request("Read 12 books", List.of()));

        assertNotNull(saved.getId());
        assertEquals(0, saved.getMilestones().size());
    }

    @Test
    void createGoalIgnoresBlankMilestoneTitles() {
        register("goal3@test.com");
        asUser("goal3@test.com");

        Goal saved = goalService.createGoal(request("Meal prep weekly", List.of("  ", "Plan meals", "", "Cook on Sunday")));

        assertEquals(2, saved.getMilestones().size());
        assertEquals(List.of("Plan meals", "Cook on Sunday"),
                saved.getMilestones().stream().map(Milestone::getTitle).toList());
    }

    @Test
    void milestonesBelongToTheirGoal() {
        register("goal4@test.com");
        asUser("goal4@test.com");

        Goal first = goalService.createGoal(request("First goal", List.of("Step A")));
        Goal second = goalService.createGoal(request("Second goal", List.of("Step B")));

        Goal reloadedFirst = goalRepository.findById(first.getId()).orElseThrow();
        Goal reloadedSecond = goalRepository.findById(second.getId()).orElseThrow();
        assertEquals(1, reloadedFirst.getMilestones().size());
        assertEquals(1, reloadedSecond.getMilestones().size());
        assertEquals("Step A", reloadedFirst.getMilestones().get(0).getTitle());
        assertEquals("Step B", reloadedSecond.getMilestones().get(0).getTitle());
    }
}
