package com.prototype.config;

import com.prototype.entity.ThirdPartyIntegration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for default OAuth settings for each integration type
 * In production, these should be stored in environment variables or a config file
 */
@Component
public class IntegrationConfig {
    
    // Default OAuth client IDs (should be configured per environment)
    // For now, these are placeholders - users will need to configure their own
    private static final Map<ThirdPartyIntegration.IntegrationType, String> DEFAULT_CLIENT_IDS = new HashMap<>();
    private static final Map<ThirdPartyIntegration.IntegrationType, String> DEFAULT_API_ENDPOINTS = new HashMap<>();
    
    static {
        // Atlassian
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.JIRA, "");
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.CONFLUENCE, "");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.JIRA, "https://api.atlassian.com");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.CONFLUENCE, "https://api.atlassian.com");
        
        // Slack
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.SLACK, "");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.SLACK, "https://slack.com/api");
        
        // GitHub
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.GITHUB, "");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.GITHUB, "https://api.github.com");
        
        // GitLab
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.GITLAB, "");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.GITLAB, "https://gitlab.com/api/v4");
        
        // Google Workspace
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.GOOGLE_DRIVE, "");
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.GOOGLE_DOCS, "");
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.GOOGLE_SHEETS, "");
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.GMAIL, "");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.GOOGLE_DRIVE, "https://www.googleapis.com");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.GOOGLE_DOCS, "https://www.googleapis.com");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.GOOGLE_SHEETS, "https://www.googleapis.com");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.GMAIL, "https://www.googleapis.com");
        
        // Microsoft
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.TEAMS, "");
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.ONEDRIVE, "");
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.WORD, "");
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.EXCEL, "");
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.OUTLOOK, "");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.TEAMS, "https://graph.microsoft.com");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.ONEDRIVE, "https://graph.microsoft.com");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.WORD, "https://graph.microsoft.com");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.EXCEL, "https://graph.microsoft.com");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.OUTLOOK, "https://graph.microsoft.com");
        
        // Salesforce
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.SALESFORCE, "");
        DEFAULT_CLIENT_IDS.put(ThirdPartyIntegration.IntegrationType.SERVICE_CLOUD, "");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.SALESFORCE, "https://login.salesforce.com");
        DEFAULT_API_ENDPOINTS.put(ThirdPartyIntegration.IntegrationType.SERVICE_CLOUD, "https://login.salesforce.com");
    }
    
    public String getDefaultClientId(ThirdPartyIntegration.IntegrationType type) {
        return DEFAULT_CLIENT_IDS.getOrDefault(type, "");
    }
    
    public String getDefaultApiEndpoint(ThirdPartyIntegration.IntegrationType type) {
        return DEFAULT_API_ENDPOINTS.getOrDefault(type, "");
    }
    
    /**
     * Get available integrations with their display information
     */
    public static Map<String, Object> getAvailableIntegrations() {
        Map<String, Object> integrations = new HashMap<>();
        
        // Atlassian
        integrations.put("JIRA", Map.of(
            "name", "Jira",
            "description", "Connect your Jira workspace to access issues and projects",
            "icon", "🔵",
            "category", "Atlassian"
        ));
        integrations.put("CONFLUENCE", Map.of(
            "name", "Confluence",
            "description", "Access your Confluence pages and documentation",
            "icon", "📘",
            "category", "Atlassian"
        ));
        integrations.put("SLACK", Map.of(
            "name", "Slack",
            "description", "Connect Slack workspaces to access channels and messages",
            "icon", "💬",
            "category", "Communication"
        ));
        
        // GitHub/GitLab
        integrations.put("GITHUB", Map.of(
            "name", "GitHub",
            "description", "Access repositories, issues, and pull requests",
            "icon", "🐙",
            "category", "Development"
        ));
        integrations.put("GITLAB", Map.of(
            "name", "GitLab",
            "description", "Connect GitLab repositories and issues",
            "icon", "🦊",
            "category", "Development"
        ));
        
        // Google Workspace
        integrations.put("GOOGLE_DRIVE", Map.of(
            "name", "Google Drive",
            "description", "Access files and documents from Google Drive",
            "icon", "📁",
            "category", "Google Workspace"
        ));
        integrations.put("GOOGLE_DOCS", Map.of(
            "name", "Google Docs",
            "description", "Read and search Google Docs documents",
            "icon", "📄",
            "category", "Google Workspace"
        ));
        integrations.put("GOOGLE_SHEETS", Map.of(
            "name", "Google Sheets",
            "description", "Access spreadsheet data from Google Sheets",
            "icon", "📊",
            "category", "Google Workspace"
        ));
        integrations.put("GMAIL", Map.of(
            "name", "Gmail",
            "description", "Access email content and threads",
            "icon", "✉️",
            "category", "Google Workspace"
        ));
        
        // Microsoft
        integrations.put("TEAMS", Map.of(
            "name", "Microsoft Teams",
            "description", "Connect Teams workspaces and channels",
            "icon", "👥",
            "category", "Microsoft"
        ));
        integrations.put("ONEDRIVE", Map.of(
            "name", "OneDrive",
            "description", "Access files from OneDrive",
            "icon", "☁️",
            "category", "Microsoft"
        ));
        integrations.put("OUTLOOK", Map.of(
            "name", "Outlook",
            "description", "Access Outlook emails and calendar",
            "icon", "📧",
            "category", "Microsoft"
        ));
        
        // Salesforce
        integrations.put("SALESFORCE", Map.of(
            "name", "Salesforce",
            "description", "Connect Salesforce org to access records",
            "icon", "⚡",
            "category", "Salesforce"
        ));
        integrations.put("SERVICE_CLOUD", Map.of(
            "name", "Service Cloud",
            "description", "Access Service Cloud cases and knowledge",
            "icon", "🎫",
            "category", "Salesforce"
        ));
        
        return integrations;
    }
}

