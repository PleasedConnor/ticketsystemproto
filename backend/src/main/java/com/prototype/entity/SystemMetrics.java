package com.prototype.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_metrics")
public class SystemMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
    
    @Column(name = "active_users")
    private Integer activeUsers;
    
    @Column(name = "total_tickets")
    private Integer totalTickets;
    
    @Column(name = "open_tickets")
    private Integer openTickets;
    
    @Column(name = "resolved_tickets")
    private Integer resolvedTickets;
    
    @Column(name = "average_resolution_time_hours")
    private Double averageResolutionTimeHours;
    
    @Column(name = "messages_per_hour")
    private Double messagesPerHour;
    
    @Column(name = "average_sentiment_score")
    private Double averageSentimentScore;
    
    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;
    
    @Column(name = "memory_usage_percent")
    private Double memoryUsagePercent;
    
    @Column(name = "response_time_ms")
    private Integer responseTimeMs;
    
    // Constructors
    public SystemMetrics() {
        this.recordedAt = LocalDateTime.now();
    }
    
    public SystemMetrics(Integer activeUsers, Integer totalTickets, Integer openTickets, 
                        Integer resolvedTickets, Double averageResolutionTimeHours) {
        this();
        this.activeUsers = activeUsers;
        this.totalTickets = totalTickets;
        this.openTickets = openTickets;
        this.resolvedTickets = resolvedTickets;
        this.averageResolutionTimeHours = averageResolutionTimeHours;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    
    public Integer getActiveUsers() { return activeUsers; }
    public void setActiveUsers(Integer activeUsers) { this.activeUsers = activeUsers; }
    
    public Integer getTotalTickets() { return totalTickets; }
    public void setTotalTickets(Integer totalTickets) { this.totalTickets = totalTickets; }
    
    public Integer getOpenTickets() { return openTickets; }
    public void setOpenTickets(Integer openTickets) { this.openTickets = openTickets; }
    
    public Integer getResolvedTickets() { return resolvedTickets; }
    public void setResolvedTickets(Integer resolvedTickets) { this.resolvedTickets = resolvedTickets; }
    
    public Double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
    public void setAverageResolutionTimeHours(Double averageResolutionTimeHours) { 
        this.averageResolutionTimeHours = averageResolutionTimeHours; 
    }
    
    public Double getMessagesPerHour() { return messagesPerHour; }
    public void setMessagesPerHour(Double messagesPerHour) { this.messagesPerHour = messagesPerHour; }
    
    public Double getAverageSentimentScore() { return averageSentimentScore; }
    public void setAverageSentimentScore(Double averageSentimentScore) { 
        this.averageSentimentScore = averageSentimentScore; 
    }
    
    public Double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(Double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    
    public Double getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(Double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }
    
    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; }
}
