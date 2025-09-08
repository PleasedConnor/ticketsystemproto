package com.prototype.repository;

import com.prototype.entity.TicketResolution;
import com.prototype.entity.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketResolutionRepository extends JpaRepository<TicketResolution, Long> {
    
    @Query("SELECT AVG(tr.resolutionTimeHours) FROM TicketResolution tr WHERE tr.resolutionTimeHours IS NOT NULL")
    Double findAverageResolutionTimeHours();
    
    @Query("SELECT AVG(tr.responseTimeMinutes) FROM TicketResolution tr WHERE tr.responseTimeMinutes IS NOT NULL")
    Double findAverageResponseTimeMinutes();
    
    @Query("SELECT AVG(tr.customerSatisfactionScore) FROM TicketResolution tr WHERE tr.customerSatisfactionScore IS NOT NULL")
    Double findAverageCustomerSatisfactionScore();
    
    @Query("SELECT t.priority, AVG(tr.resolutionTimeHours) FROM TicketResolution tr JOIN tr.ticket t WHERE tr.resolutionTimeHours IS NOT NULL GROUP BY t.priority")
    List<Object[]> findAverageResolutionTimeByPriority();
    
    @Query("SELECT t.status, AVG(tr.resolutionTimeHours) FROM TicketResolution tr JOIN tr.ticket t WHERE tr.resolutionTimeHours IS NOT NULL GROUP BY t.status")
    List<Object[]> findAverageResolutionTimeByStatus();
    
    @Query("SELECT u.location, AVG(tr.resolutionTimeHours) FROM TicketResolution tr JOIN tr.ticket t JOIN t.user u WHERE tr.resolutionTimeHours IS NOT NULL GROUP BY u.location")
    List<Object[]> findAverageResolutionTimeByLocation();
    
    @Query("SELECT COUNT(tr) FROM TicketResolution tr WHERE tr.resolvedAt IS NOT NULL")
    Long countResolvedTickets();
}
