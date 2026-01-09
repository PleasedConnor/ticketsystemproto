package com.prototype.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_configurations")
public class AIConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Three main configuration sections with priority hierarchy:
    // 1. General AI Rules (lowest priority, always present)
    // 2. Escalation Processes (overrides General Rules)
    // 3. Edge Case AI Rules (overrides both General Rules and Escalation Processes)
    
    @Column(name = "general_rules", columnDefinition = "TEXT")
    private String generalRules; // Always present, lowest priority
    
    @Column(name = "escalation_processes", columnDefinition = "TEXT")
    private String escalationProcesses; // Overrides General Rules
    
    @Column(name = "edge_case_rules", columnDefinition = "TEXT")
    private String edgeCaseRules; // Overrides both General Rules and Escalation Processes
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public AIConfiguration() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getGeneralRules() { return generalRules; }
    public void setGeneralRules(String generalRules) { this.generalRules = generalRules; }
    
    public String getEscalationProcesses() { return escalationProcesses; }
    public void setEscalationProcesses(String escalationProcesses) { this.escalationProcesses = escalationProcesses; }
    
    public String getEdgeCaseRules() { return edgeCaseRules; }
    public void setEdgeCaseRules(String edgeCaseRules) { this.edgeCaseRules = edgeCaseRules; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

