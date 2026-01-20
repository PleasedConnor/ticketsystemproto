package com.prototype.service;

import com.prototype.entity.ThirdPartyIntegration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OAuthService {
    
    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;
    
    @Value("${app.backend-url:http://localhost:8080/api}")
    private String backendUrl;
    
    /**
     * Generate OAuth authorization URL for a given integration type
     */
    public Map<String, String> generateOAuthUrl(ThirdPartyIntegration.IntegrationType integrationType, 
                                                 String clientId, 
                                                 String workspaceId,
                                                 String state) {
        Map<String, String> result = new HashMap<>();
        
        String redirectUri = backendUrl + "/third-party-integrations/oauth/callback";
        String scope = getScopesForIntegration(integrationType);
        String authUrl = buildAuthUrl(integrationType, clientId, redirectUri, scope, workspaceId, state);
        
        result.put("authUrl", authUrl);
        result.put("state", state);
        result.put("redirectUri", redirectUri);
        
        return result;
    }
    
    /**
     * Get required OAuth scopes for each integration type
     */
    private String getScopesForIntegration(ThirdPartyIntegration.IntegrationType integrationType) {
        switch (integrationType) {
            case JIRA:
                return "read:jira-work read:jira-user offline_access";
            case CONFLUENCE:
                return "read:confluence-content.all read:confluence-space.summary offline_access";
            case SLACK:
                return "channels:read groups:read im:read mpim:read files:read";
            case GITHUB:
                return "repo read:org read:user";
            case GITLAB:
                return "read_api read_repository";
            case GOOGLE_DRIVE:
            case GOOGLE_DOCS:
            case GOOGLE_SHEETS:
                return "https://www.googleapis.com/auth/drive.readonly https://www.googleapis.com/auth/documents.readonly https://www.googleapis.com/auth/spreadsheets.readonly";
            case GMAIL:
                return "https://www.googleapis.com/auth/gmail.readonly";
            case TEAMS:
            case ONEDRIVE:
            case WORD:
            case EXCEL:
            case OUTLOOK:
                return "Files.Read.All Sites.Read.All User.Read";
            case SALESFORCE:
            case SERVICE_CLOUD:
                return "api refresh_token offline_access";
            default:
                return "read";
        }
    }
    
    /**
     * Build OAuth authorization URL for each service
     */
    private String buildAuthUrl(ThirdPartyIntegration.IntegrationType integrationType,
                               String clientId,
                               String redirectUri,
                               String scope,
                               String workspaceId,
                               String state) {
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);
        String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
        
        switch (integrationType) {
            case JIRA:
            case CONFLUENCE:
                // Atlassian OAuth
                return String.format(
                    "https://auth.atlassian.com/authorize?audience=api.atlassian.com&client_id=%s&scope=%s&redirect_uri=%s&state=%s&response_type=code&prompt=consent",
                    clientId, encodedScope, encodedRedirect, encodedState
                );
                
            case SLACK:
                return String.format(
                    "https://slack.com/oauth/v2/authorize?client_id=%s&scope=%s&redirect_uri=%s&state=%s",
                    clientId, encodedScope, encodedRedirect, encodedState
                );
                
            case GITHUB:
                return String.format(
                    "https://github.com/login/oauth/authorize?client_id=%s&scope=%s&redirect_uri=%s&state=%s",
                    clientId, encodedScope, encodedRedirect, encodedState
                );
                
            case GITLAB:
                return String.format(
                    "https://gitlab.com/oauth/authorize?client_id=%s&scope=%s&redirect_uri=%s&state=%s&response_type=code",
                    clientId, encodedScope, encodedRedirect, encodedState
                );
                
            case GOOGLE_DRIVE:
            case GOOGLE_DOCS:
            case GOOGLE_SHEETS:
            case GMAIL:
                return String.format(
                    "https://accounts.google.com/o/oauth2/v2/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s&access_type=offline&prompt=consent",
                    clientId, encodedRedirect, encodedScope, encodedState
                );
                
            case TEAMS:
            case ONEDRIVE:
            case WORD:
            case EXCEL:
            case OUTLOOK:
                return String.format(
                    "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=%s&response_type=code&redirect_uri=%s&response_mode=query&scope=%s&state=%s",
                    clientId, encodedRedirect, encodedScope, encodedState
                );
                
            case SALESFORCE:
            case SERVICE_CLOUD:
                String loginUrl = workspaceId != null ? workspaceId : "https://login.salesforce.com";
                return String.format(
                    "%s/services/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                    loginUrl, clientId, encodedRedirect, encodedScope, encodedState
                );
                
            default:
                throw new IllegalArgumentException("OAuth not supported for integration type: " + integrationType);
        }
    }
    
    /**
     * Generate a unique state token for OAuth flow
     */
    public String generateState() {
        return UUID.randomUUID().toString();
    }
}

