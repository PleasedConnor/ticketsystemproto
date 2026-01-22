package com.prototype.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_actions")
public class ChatbotAction {
    
    public enum ComponentType {
        ORDER_SELECTOR,
        DATE_PICKER,
        FILE_UPLOADER,
        FORM,
        BUTTON,
        CUSTOM
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name; // User-friendly name (e.g., "Order Status Selector")
    
    @Column(name = "action_key", nullable = false, unique = true, length = 100)
    private String actionKey; // Unique key for referencing in AI config (e.g., "order_selector")
    
    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 50)
    private ComponentType componentType;
    
    @Column(name = "config", columnDefinition = "TEXT")
    private String config; // JSON string for component-specific configuration
    
    @Column(name = "description", length = 500)
    private String description; // Description of what this action does
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public ChatbotAction() {
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
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    
    public ComponentType getComponentType() { return componentType; }
    public void setComponentType(ComponentType componentType) { this.componentType = componentType; }
    
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

