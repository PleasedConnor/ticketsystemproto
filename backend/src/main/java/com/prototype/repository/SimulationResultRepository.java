package com.prototype.repository;

import com.prototype.entity.SimulationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationResultRepository extends JpaRepository<SimulationResult, Long> {
    List<SimulationResult> findByTicketId(Long ticketId);
    List<SimulationResult> findBySimulationRunId(String simulationRunId);
    Optional<SimulationResult> findByTicketIdAndSimulationRunId(Long ticketId, String simulationRunId);
    void deleteBySimulationRunId(String simulationRunId);
}

