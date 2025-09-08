package com.prototype.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_sentiment")
public class MessageSentiment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "message_id", referencedColumnName = "id")
    private TicketMessage message;
    
    @Column(name = "sentiment_score", nullable = false)
    private Double sentimentScore; // Range: -1.0 to +1.0
    
    @Column(name = "sentiment_label", nullable = false)
    @Enumerated(EnumType.STRING)
    private SentimentLabel sentimentLabel;
    
    @Column(name = "confidence_score")
    private Double confidenceScore; // Range: 0.0 to 1.0
    
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
    
    // Constructors
    public MessageSentiment() {
        this.analyzedAt = LocalDateTime.now();
    }
    
    public MessageSentiment(TicketMessage message, Double sentimentScore, SentimentLabel sentimentLabel, Double confidenceScore) {
        this();
        this.message = message;
        this.sentimentScore = sentimentScore;
        this.sentimentLabel = sentimentLabel;
        this.confidenceScore = confidenceScore;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public TicketMessage getMessage() { return message; }
    public void setMessage(TicketMessage message) { this.message = message; }
    
    public Double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }
    
    public SentimentLabel getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(SentimentLabel sentimentLabel) { this.sentimentLabel = sentimentLabel; }
    
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}
