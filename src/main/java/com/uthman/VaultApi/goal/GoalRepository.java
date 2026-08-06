package com.uthman.VaultApi.goal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserId(Long userId);

    Page<Goal> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT g FROM Goal g
            WHERE g.user.id = :userId
              AND (:search IS NULL OR :search = ''
                   OR LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(g.description) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR g.status = :status)
              AND (:category IS NULL OR :category = ''
                   OR LOWER(g.category) = LOWER(:category))
              AND (:tag IS NULL OR :tag = '' OR :tag MEMBER OF g.tags)
            """)
    Page<Goal> search(@Param("userId") Long userId,
                      @Param("search") String search,
                      @Param("status") Goal.Status status,
                      @Param("category") String category,
                      @Param("tag") String tag,
                      Pageable pageable);

    @Query("""
            SELECT g FROM Goal g
            WHERE g.user.id = :userId
              AND g.status = 'IN_PROGRESS'
              AND g.targetDate IS NOT NULL
              AND g.targetDate BETWEEN :from AND :to
            ORDER BY g.targetDate ASC
            """)
    List<Goal> findUpcoming(@Param("userId") Long userId,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to);

    Optional<Goal> findByIdAndIsPublicTrue(Long id);

    List<Goal> findByUserIdOrderByCreatedAtDesc(Long userId);
}
