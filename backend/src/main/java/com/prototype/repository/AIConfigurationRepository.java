package com.prototype.repository;

import com.prototype.entity.AIConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIConfigurationRepository extends JpaRepository<AIConfiguration, Long> {
    // Simple repository - only need basic CRUD operations
}

