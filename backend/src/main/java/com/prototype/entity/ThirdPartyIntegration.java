package com.prototype.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "third_party_integrations")
public class ThirdPartyIntegration {
    
    public enum IntegrationType {
        // Atlassian
        JIRA,
        CONFLUENCE,
        SLACK,
        
        // Salesforce
        SALESFORCE,
        SERVICE_CLOUD,
        
        // GitHub/GitLab
        GITHUB,
        GITLAB,
        
        // Microsoft
        TEAMS,
        ONEDRIVE,
        WORD,
        EXCEL,
        OUTLOOK,
        
        // Google Workspace
        GOOGLE_DRIVE,
        GOOGLE_DOCS,
        GOOGLE_SHEETS,
        GMAIL
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name; // User-friendly name for the integration
    
    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 50)
    private IntegrationType integrationType;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "ai_accessible", nullable = false)
    private Boolean aiAccessible = true; // Whether AI can use data from this integration
    
    // OAuth/API Credentials (encrypted in production)
    @Column(name = "auth_type", length = 50)
    private String authType; // OAUTH2, API_KEY, BASIC, BEARER
    
    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint; // Base API URL
    
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken; // OAuth access token or API key
    
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken; // OAuth refresh token
    
    @Column(name = "client_id", length = 200)
    private String clientId;
    
    @Column(name = "client_secret", columnDefinition = "TEXT")
    private String clientSecret;
    
    @Column(name = "workspace_id", length = 200)
    private String workspaceId; // For multi-tenant services (e.g., Slack workspace, Jira site)
    
    @Column(name = "additional_config", columnDefinition = "TEXT")
    private String additionalConfig; // JSON string for service-specific config
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt; // Last time data was synced
    
    @Column(name = "sync_enabled", nullable = false)
    private Boolean syncEnabled = true; // Whether to automatically sync data
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "integration", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<BlacklistedItem> blacklistedItems;
    
    public ThirdPartyIntegration() {
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
    
    public IntegrationType getIntegrationType() { return integrationType; }
    public void setIntegrationType(IntegrationType integrationType) { this.integrationType = integrationType; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getAiAccessible() { return aiAccessible; }
    public void setAiAccessible(Boolean aiAccessible) { this.aiAccessible = aiAccessible; }
    
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    
    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
    
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    
    public String getAdditionalConfig() { return additionalConfig; }
    public void setAdditionalConfig(String additionalConfig) { this.additionalConfig = additionalConfig; }
    
    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    
    public Boolean getSyncEnabled() { return syncEnabled; }
    public void setSyncEnabled(Boolean syncEnabled) { this.syncEnabled = syncEnabled; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<BlacklistedItem> getBlacklistedItems() { return blacklistedItems; }
    public void setBlacklistedItems(List<BlacklistedItem> blacklistedItems) { this.blacklistedItems = blacklistedItems; }
}

