package com.prototype.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_results")
public class SimulationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    
    @Column(name = "ticket_subject", nullable = false)
    private String ticketSubject;
    
    @Column(name = "human_reply", columnDefinition = "TEXT")
    private String humanReply;
    
    @Column(name = "ai_reply", columnDefinition = "TEXT")
    private String aiReply;
    
    @Column(name = "ai_self_reflection_correct", nullable = false)
    private Boolean aiSelfReflectionCorrect;
    
    @Column(name = "ai_self_reflection_confidence", nullable = false)
    private Double aiSelfReflectionConfidence; // 0.0 to 1.0
    
    @Column(name = "ai_notes", columnDefinition = "TEXT")
    private String aiNotes;
    
    @Column(name = "human_mark")
    @Enumerated(EnumType.STRING)
    private HumanMark humanMark;
    
    @Column(name = "human_notes", columnDefinition = "TEXT")
    private String humanNotes;
    
    @Column(name = "ideal_response", columnDefinition = "TEXT")
    private String idealResponse;
    
    @Column(name = "model_name")
    private String modelName;
    
    @Column(name = "simulation_run_id")
    private String simulationRunId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum HumanMark {
        CORRECT, INCORRECT
    }
    
    // Constructors
    public SimulationResult() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    
    public String getTicketSubject() { return ticketSubject; }
    public void setTicketSubject(String ticketSubject) { this.ticketSubject = ticketSubject; }
    
    public String getHumanReply() { return humanReply; }
    public void setHumanReply(String humanReply) { this.humanReply = humanReply; }
    
    public String getAiReply() { return aiReply; }
    public void setAiReply(String aiReply) { this.aiReply = aiReply; }
    
    public Boolean getAiSelfReflectionCorrect() { return aiSelfReflectionCorrect; }
    public void setAiSelfReflectionCorrect(Boolean aiSelfReflectionCorrect) { 
        this.aiSelfReflectionCorrect = aiSelfReflectionCorrect; 
    }
    
    public Double getAiSelfReflectionConfidence() { return aiSelfReflectionConfidence; }
    public void setAiSelfReflectionConfidence(Double aiSelfReflectionConfidence) { 
        this.aiSelfReflectionConfidence = aiSelfReflectionConfidence; 
    }
    
    public String getAiNotes() { return aiNotes; }
    public void setAiNotes(String aiNotes) { this.aiNotes = aiNotes; }
    
    public HumanMark getHumanMark() { return humanMark; }
    public void setHumanMark(HumanMark humanMark) { this.humanMark = humanMark; }
    
    public String getHumanNotes() { return humanNotes; }
    public void setHumanNotes(String humanNotes) { this.humanNotes = humanNotes; }
    
    public String getIdealResponse() { return idealResponse; }
    public void setIdealResponse(String idealResponse) { this.idealResponse = idealResponse; }
    
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    
    public String getSimulationRunId() { return simulationRunId; }
    public void setSimulationRunId(String simulationRunId) { this.simulationRunId = simulationRunId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

