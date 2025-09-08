package com.prototype.repository;

import com.prototype.entity.Ticket;
import com.prototype.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserUid(String userUid);
    List<Ticket> findByStatus(TicketStatus status);
    
    @Query("SELECT t FROM Ticket t JOIN FETCH t.user ORDER BY t.createdAt DESC")
    List<Ticket> findAllWithUser();
}
