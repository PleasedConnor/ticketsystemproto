package com.prototype.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "user_activities")
public class UserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_uid", referencedColumnName = "uid")
    private User user;
    
    @Column(name = "activity_date")
    private LocalDate activityDate;
    
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
    
    @Column(name = "tickets_created")
    private Integer ticketsCreated = 0;
    
    @Column(name = "messages_sent")
    private Integer messagesSent = 0;
    
    @Column(name = "session_count")
    private Integer sessionCount = 0;
    
    @Column(name = "total_session_minutes")
    private Long totalSessionMinutes = 0L;
    
    // Constructors
    public UserActivity() {}
    
    public UserActivity(User user, LocalDate activityDate) {
        this.user = user;
        this.activityDate = activityDate;
        this.lastActiveAt = LocalDateTime.now();
    }
    
    // Helper methods
    public void recordTicketCreated() {
        this.ticketsCreated++;
        this.lastActiveAt = LocalDateTime.now();
    }
    
    public void recordMessageSent() {
        this.messagesSent++;
        this.lastActiveAt = LocalDateTime.now();
    }
    
    public void recordSession(long durationMinutes) {
        this.sessionCount++;
        this.totalSessionMinutes += durationMinutes;
        this.lastActiveAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }
    
    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    
    public Integer getTicketsCreated() { return ticketsCreated; }
    public void setTicketsCreated(Integer ticketsCreated) { this.ticketsCreated = ticketsCreated; }
    
    public Integer getMessagesSent() { return messagesSent; }
    public void setMessagesSent(Integer messagesSent) { this.messagesSent = messagesSent; }
    
    public Integer getSessionCount() { return sessionCount; }
    public void setSessionCount(Integer sessionCount) { this.sessionCount = sessionCount; }
    
    public Long getTotalSessionMinutes() { return totalSessionMinutes; }
    public void setTotalSessionMinutes(Long totalSessionMinutes) { this.totalSessionMinutes = totalSessionMinutes; }
}
