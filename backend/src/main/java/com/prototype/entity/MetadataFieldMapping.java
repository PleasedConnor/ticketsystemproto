package com.prototype.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "metadata_field_mappings")
public class MetadataFieldMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", nullable = false)
    @JsonIgnore // Prevent circular serialization
    private MetadataConnection connection;
    
    @Column(name = "external_field_path", nullable = false, length = 500)
    private String externalFieldPath; // e.g., "username.name", "user.email", "order.id"
    
    @Column(name = "internal_variable", nullable = false, length = 100)
    private String internalVariable; // e.g., "{{user.name}}", "{{user.email}}", "{{order.id}}"
    
    @Column(name = "data_type", length = 50)
    private String dataType; // STRING, NUMBER, BOOLEAN, DATE, etc.
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Optional description
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "ai_accessible")
    private Boolean aiAccessible = true; // Default to true for backward compatibility
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public MetadataFieldMapping() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public MetadataConnection getConnection() { return connection; }
    public void setConnection(MetadataConnection connection) { this.connection = connection; }
    
    // Helper method to get connection name for JSON serialization
    @com.fasterxml.jackson.annotation.JsonGetter("connectionName")
    public String getConnectionName() {
        return connection != null ? connection.getName() : null;
    }
    
    public String getExternalFieldPath() { return externalFieldPath; }
    public void setExternalFieldPath(String externalFieldPath) { this.externalFieldPath = externalFieldPath; }
    
    public String getInternalVariable() { return internalVariable; }
    public void setInternalVariable(String internalVariable) { this.internalVariable = internalVariable; }
    
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getAiAccessible() { return aiAccessible; }
    public void setAiAccessible(Boolean aiAccessible) { this.aiAccessible = aiAccessible; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

