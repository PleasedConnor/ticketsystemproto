package com.prototype.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "ticket_resolutions")
public class TicketResolution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "ticket_id", referencedColumnName = "id")
    private Ticket ticket;
    
    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    @Column(name = "response_time_minutes")
    private Long responseTimeMinutes; // Time to first response
    
    @Column(name = "resolution_time_hours")
    private Long resolutionTimeHours; // Time to resolution
    
    @Column(name = "total_messages")
    private Integer totalMessages;
    
    @Column(name = "customer_satisfaction_score")
    private Double customerSatisfactionScore; // 1.0 to 5.0
    
    @Column(name = "resolved_by")
    private String resolvedBy; // Agent name
    
    // Constructors
    public TicketResolution() {}
    
    public TicketResolution(Ticket ticket) {
        this.ticket = ticket;
        // Don't calculate metrics here to avoid LazyInitializationException
        // Metrics will be calculated manually in DataInitializer
    }
    
    // Method to calculate metrics based on ticket data
    public void calculateMetrics() {
        if (ticket == null) return;
        
        LocalDateTime createdAt = ticket.getCreatedAt();
        
        // Find first response time (first message from support)
        if (ticket.getMessages() != null) {
            ticket.getMessages().stream()
                .filter(msg -> msg.getSenderType() == SenderType.AGENT)
                .min((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .ifPresent(firstResponse -> {
                    this.firstResponseAt = firstResponse.getCreatedAt();
                    this.responseTimeMinutes = Duration.between(createdAt, firstResponseAt).toMinutes();
                });
                
            this.totalMessages = ticket.getMessages().size();
        }
        
        // Set resolution time if ticket is resolved or closed
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            LocalDateTime resolutionTime = ticket.getUpdatedAt();
            this.resolvedAt = resolutionTime;
            this.resolutionTimeHours = Duration.between(createdAt, resolutionTime).toHours();
            
            if (ticket.getStatus() == TicketStatus.CLOSED) {
                this.closedAt = resolutionTime;
            }
        }
        
        // Generate realistic customer satisfaction score based on resolution time and priority
        generateSatisfactionScore();
    }
    
    private void generateSatisfactionScore() {
        if (resolutionTimeHours == null) return;
        
        double baseScore = 4.0; // Start with good score
        
        // Adjust based on priority and resolution time
        switch (ticket.getPriority()) {
            case URGENT:
                if (resolutionTimeHours > 4) baseScore -= 1.5;
                else if (resolutionTimeHours > 2) baseScore -= 0.5;
                break;
            case HIGH:
                if (resolutionTimeHours > 8) baseScore -= 1.0;
                else if (resolutionTimeHours > 4) baseScore -= 0.3;
                break;
            case MEDIUM:
                if (resolutionTimeHours > 24) baseScore -= 0.8;
                else if (resolutionTimeHours > 12) baseScore -= 0.2;
                break;
            case LOW:
                if (resolutionTimeHours > 72) baseScore -= 0.5;
                break;
        }
        
        // Add some randomness but keep it realistic
        baseScore += (Math.random() - 0.5) * 0.6; // ±0.3 variation
        
        // Clamp between 1.0 and 5.0
        this.customerSatisfactionScore = Math.max(1.0, Math.min(5.0, baseScore));
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    
    public LocalDateTime getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(LocalDateTime firstResponseAt) { this.firstResponseAt = firstResponseAt; }
    
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    
    public Long getResponseTimeMinutes() { return responseTimeMinutes; }
    public void setResponseTimeMinutes(Long responseTimeMinutes) { this.responseTimeMinutes = responseTimeMinutes; }
    
    public Long getResolutionTimeHours() { return resolutionTimeHours; }
    public void setResolutionTimeHours(Long resolutionTimeHours) { this.resolutionTimeHours = resolutionTimeHours; }
    
    public Integer getTotalMessages() { return totalMessages; }
    public void setTotalMessages(Integer totalMessages) { this.totalMessages = totalMessages; }
    
    public Double getCustomerSatisfactionScore() { return customerSatisfactionScore; }
    public void setCustomerSatisfactionScore(Double customerSatisfactionScore) { this.customerSatisfactionScore = customerSatisfactionScore; }
    
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
}
