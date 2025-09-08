package com.prototype.repository;

import com.prototype.entity.MessageSentiment;
import com.prototype.entity.SentimentLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageSentimentRepository extends JpaRepository<MessageSentiment, Long> {
    
    List<MessageSentiment> findBySentimentLabel(SentimentLabel label);
    
    @Query("SELECT AVG(ms.sentimentScore) FROM MessageSentiment ms")
    Double findAverageSentimentScore();
    
    @Query("SELECT COUNT(ms) FROM MessageSentiment ms WHERE ms.sentimentLabel = ?1")
    Long countBySentimentLabel(SentimentLabel label);
    
    @Query("SELECT ms.sentimentLabel, COUNT(ms) FROM MessageSentiment ms GROUP BY ms.sentimentLabel")
    List<Object[]> findSentimentDistribution();
    
    @Query("SELECT AVG(ms.sentimentScore) FROM MessageSentiment ms JOIN ms.message m JOIN m.ticket t WHERE t.status = ?1")
    Double findAverageSentimentByTicketStatus(com.prototype.entity.TicketStatus status);
}
