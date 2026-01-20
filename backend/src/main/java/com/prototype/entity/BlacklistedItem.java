package com.prototype.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_items")
public class BlacklistedItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_id", nullable = false)
    @JsonIgnore
    private ThirdPartyIntegration integration;
    
    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType; // PAGE, ISSUE, CHANNEL, DOCUMENT, etc.
    
    @Column(name = "item_id", nullable = false, length = 200)
    private String itemId; // External ID of the item (e.g., Confluence page ID, Jira issue key)
    
    @Column(name = "item_title", length = 500)
    private String itemTitle; // Human-readable title for display
    
    @Column(name = "item_url", length = 1000)
    private String itemUrl; // URL to the item in the external system
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // Optional reason for blacklisting
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public BlacklistedItem() {
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
    
    public ThirdPartyIntegration getIntegration() { return integration; }
    public void setIntegration(ThirdPartyIntegration integration) { this.integration = integration; }
    
    @com.fasterxml.jackson.annotation.JsonGetter("integrationId")
    public Long getIntegrationId() {
        return integration != null ? integration.getId() : null;
    }
    
    @com.fasterxml.jackson.annotation.JsonGetter("integrationName")
    public String getIntegrationName() {
        return integration != null ? integration.getName() : null;
    }
    
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    
    public String getItemTitle() { return itemTitle; }
    public void setItemTitle(String itemTitle) { this.itemTitle = itemTitle; }
    
    public String getItemUrl() { return itemUrl; }
    public void setItemUrl(String itemUrl) { this.itemUrl = itemUrl; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

