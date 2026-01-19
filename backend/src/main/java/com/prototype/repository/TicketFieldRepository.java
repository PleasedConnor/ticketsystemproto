package com.prototype.repository;

import com.prototype.entity.TicketField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketFieldRepository extends JpaRepository<TicketField, Long> {
    @Query("SELECT f FROM TicketField f WHERE f.isVisible = true ORDER BY f.displayLocation, f.displayOrder ASC")
    List<TicketField> findAllVisibleOrdered();
    
    @Query("SELECT f FROM TicketField f WHERE f.isVisible = true AND f.displayLocation = :location ORDER BY f.displayOrder ASC")
    List<TicketField> findByLocationAndVisible(@Param("location") String location);
}

