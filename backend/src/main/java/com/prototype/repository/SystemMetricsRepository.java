package com.prototype.repository;

import com.prototype.entity.SystemMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemMetricsRepository extends JpaRepository<SystemMetrics, Long> {
    
    List<SystemMetrics> findByRecordedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    List<SystemMetrics> findTop24ByOrderByRecordedAtDesc();
    
    @Query("SELECT AVG(sm.responseTimeMs) FROM SystemMetrics sm WHERE sm.recordedAt BETWEEN ?1 AND ?2")
    Double findAverageResponseTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT AVG(sm.cpuUsagePercent) FROM SystemMetrics sm WHERE sm.recordedAt BETWEEN ?1 AND ?2")
    Double findAverageCpuUsageBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT AVG(sm.memoryUsagePercent) FROM SystemMetrics sm WHERE sm.recordedAt BETWEEN ?1 AND ?2")
    Double findAverageMemoryUsageBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT MAX(sm.activeUsers) FROM SystemMetrics sm WHERE sm.recordedAt BETWEEN ?1 AND ?2")
    Integer findMaxActiveUsersBetween(LocalDateTime startTime, LocalDateTime endTime);
}
