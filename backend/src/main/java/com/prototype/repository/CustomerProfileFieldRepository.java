package com.prototype.repository;

import com.prototype.entity.CustomerProfileField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerProfileFieldRepository extends JpaRepository<CustomerProfileField, Long> {
    List<CustomerProfileField> findByIsVisibleTrueOrderByDisplayOrderAsc();
    
    @Query("SELECT f FROM CustomerProfileField f WHERE f.isVisible = true ORDER BY f.displayOrder ASC")
    List<CustomerProfileField> findAllVisibleOrdered();
}

