package com.uthman.VaultApi.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByGoalIdOrderByIdAsc(Long goalId);
}
