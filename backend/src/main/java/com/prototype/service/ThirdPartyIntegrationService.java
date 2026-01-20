package com.prototype.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prototype.entity.BlacklistedItem;
import com.prototype.entity.ThirdPartyIntegration;
import com.prototype.repository.BlacklistedItemRepository;
import com.prototype.repository.ThirdPartyIntegrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ThirdPartyIntegrationService {
    
    @Autowired
    private ThirdPartyIntegrationRepository integrationRepository;
    
    @Autowired
    private BlacklistedItemRepository blacklistedItemRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<ThirdPartyIntegration> getAllIntegrations() {
        return integrationRepository.findAll();
    }
    
    public List<ThirdPartyIntegration> getActiveIntegrations() {
        return integrationRepository.findByIsActiveTrue();
    }
    
    public List<ThirdPartyIntegration> getAIAccessibleIntegrations() {
        return integrationRepository.findByAiAccessibleTrueAndIsActiveTrue();
    }
    
    public List<ThirdPartyIntegration> findOAuthAppConfigsByType(ThirdPartyIntegration.IntegrationType type) {
        return integrationRepository.findOAuthAppConfigsByType(type);
    }
    
    @Transactional(readOnly = true)
    public ThirdPartyIntegration getIntegrationById(Long id) {
        return integrationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Integration not found with id: " + id));
    }
    
    @Transactional
    public ThirdPartyIntegration createIntegration(ThirdPartyIntegration integration) {
        return integrationRepository.save(integration);
    }
    
    @Transactional
    public ThirdPartyIntegration updateIntegration(Long id, ThirdPartyIntegration integration) {
        ThirdPartyIntegration existing = getIntegrationById(id);
        existing.setName(integration.getName());
        existing.setIntegrationType(integration.getIntegrationType());
        existing.setIsActive(integration.getIsActive());
        existing.setAiAccessible(integration.getAiAccessible());
        existing.setAuthType(integration.getAuthType());
        existing.setApiEndpoint(integration.getApiEndpoint());
        existing.setAccessToken(integration.getAccessToken());
        existing.setRefreshToken(integration.getRefreshToken());
        existing.setClientId(integration.getClientId());
        existing.setClientSecret(integration.getClientSecret());
        existing.setWorkspaceId(integration.getWorkspaceId());
        existing.setAdditionalConfig(integration.getAdditionalConfig());
        existing.setSyncEnabled(integration.getSyncEnabled());
        return integrationRepository.save(existing);
    }
    
    @Transactional
    public void deleteIntegration(Long id) {
        // Delete all blacklisted items first
        List<BlacklistedItem> blacklistedItems = blacklistedItemRepository.findByIntegrationId(id);
        blacklistedItemRepository.deleteAll(blacklistedItems);
        
        integrationRepository.deleteById(id);
    }
    
    @Transactional
    public void updateAIAccess(Long id, Boolean aiAccessible) {
        ThirdPartyIntegration integration = getIntegrationById(id);
        integration.setAiAccessible(aiAccessible);
        integrationRepository.save(integration);
    }
    
    /**
     * Test connection to a 3rd party service
     */
    public Map<String, Object> testConnection(ThirdPartyIntegration integration) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            WebClient.RequestHeadersSpec<?> requestSpec = buildRequest(integration, "/test", "GET", null);
            
            String response = requestSpec
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
            
            result.put("success", true);
            result.put("message", "Connection successful");
            result.put("response", response);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Fetch available items from a 3rd party service (e.g., Confluence pages, Jira issues)
     */
    public List<Map<String, Object>> fetchAvailableItems(ThirdPartyIntegration integration, String itemType) {
        List<Map<String, Object>> items = new ArrayList<>();
        
        try {
            String endpoint = getItemsEndpoint(integration.getIntegrationType(), itemType);
            WebClient.RequestHeadersSpec<?> requestSpec = buildRequest(integration, endpoint, "GET", null);
            
            String response = requestSpec
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            items = parseItemsResponse(integration.getIntegrationType(), itemType, jsonResponse);
            
            // Filter out blacklisted items
            List<String> blacklistedIds = blacklistedItemRepository
                .findByIntegrationIdAndItemType(integration.getId(), itemType)
                .stream()
                .map(BlacklistedItem::getItemId)
                .collect(Collectors.toList());
            
            items = items.stream()
                .filter(item -> !blacklistedIds.contains(item.get("id").toString()))
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("Error fetching items from " + integration.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return items;
    }
    
    /**
     * Get blacklisted items for an integration
     */
    @Transactional(readOnly = true)
    public List<BlacklistedItem> getBlacklistedItems(Long integrationId) {
        return blacklistedItemRepository.findByIntegrationIdWithIntegration(integrationId);
    }
    
    /**
     * Add item to blacklist
     */
    @Transactional
    public BlacklistedItem addToBlacklist(Long integrationId, String itemType, String itemId, 
                                         String itemTitle, String itemUrl, String reason) {
        ThirdPartyIntegration integration = getIntegrationById(integrationId);
        
        // Check if already blacklisted
        if (blacklistedItemRepository.existsByIntegrationIdAndItemId(integrationId, itemId)) {
            throw new RuntimeException("Item is already blacklisted");
        }
        
        BlacklistedItem blacklistedItem = new BlacklistedItem();
        blacklistedItem.setIntegration(integration);
        blacklistedItem.setItemType(itemType);
        blacklistedItem.setItemId(itemId);
        blacklistedItem.setItemTitle(itemTitle);
        blacklistedItem.setItemUrl(itemUrl);
        blacklistedItem.setReason(reason);
        
        return blacklistedItemRepository.save(blacklistedItem);
    }
    
    /**
     * Remove item from blacklist
     */
    @Transactional
    public void removeFromBlacklist(Long integrationId, String itemId) {
        Optional<BlacklistedItem> item = blacklistedItemRepository.findByIntegrationIdAndItemId(integrationId, itemId);
        if (item.isPresent()) {
            blacklistedItemRepository.delete(item.get());
        }
    }
    
    /**
     * Bulk add items to blacklist
     */
    @Transactional
    public List<BlacklistedItem> bulkAddToBlacklist(Long integrationId, List<Map<String, String>> items) {
        ThirdPartyIntegration integration = getIntegrationById(integrationId);
        List<BlacklistedItem> blacklistedItems = new ArrayList<>();
        
        for (Map<String, String> item : items) {
            String itemId = item.get("itemId");
            
            // Skip if already blacklisted
            if (blacklistedItemRepository.existsByIntegrationIdAndItemId(integrationId, itemId)) {
                continue;
            }
            
            BlacklistedItem blacklistedItem = new BlacklistedItem();
            blacklistedItem.setIntegration(integration);
            blacklistedItem.setItemType(item.get("itemType"));
            blacklistedItem.setItemId(itemId);
            blacklistedItem.setItemTitle(item.get("itemTitle"));
            blacklistedItem.setItemUrl(item.get("itemUrl"));
            blacklistedItem.setReason(item.get("reason"));
            
            blacklistedItems.add(blacklistedItemRepository.save(blacklistedItem));
        }
        
        return blacklistedItems;
    }
    
    /**
     * Build HTTP request for 3rd party API
     */
    private WebClient.RequestHeadersSpec<?> buildRequest(ThirdPartyIntegration integration, 
                                                          String endpoint, String method, Object body) {
        String url = integration.getApiEndpoint() + endpoint;
        WebClient.Builder builder = WebClient.builder().baseUrl(url);
        
        WebClient.RequestHeadersSpec<?> requestSpec;
        
        // Add authentication
        HttpHeaders headers = new HttpHeaders();
        if (integration.getAuthType() != null) {
            switch (integration.getAuthType().toUpperCase()) {
                case "BEARER":
                case "OAUTH2":
                    if (integration.getAccessToken() != null) {
                        headers.setBearerAuth(integration.getAccessToken());
                    }
                    break;
                case "BASIC":
                    if (integration.getClientId() != null && integration.getClientSecret() != null) {
                        String credentials = integration.getClientId() + ":" + integration.getClientSecret();
                        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
                        headers.set("Authorization", "Basic " + encoded);
                    }
                    break;
                case "API_KEY":
                    if (integration.getAccessToken() != null) {
                        headers.set("X-API-Key", integration.getAccessToken());
                    }
                    break;
            }
        }
        
        WebClient client = builder.defaultHeaders(h -> {
            h.addAll(headers);
        }).build();
        
        switch (method.toUpperCase()) {
            case "GET":
                requestSpec = client.get();
                break;
            case "POST":
                requestSpec = client.post().bodyValue(body != null ? body : "");
                break;
            case "PUT":
                requestSpec = client.put().bodyValue(body != null ? body : "");
                break;
            case "DELETE":
                requestSpec = client.delete();
                break;
            default:
                requestSpec = client.get();
        }
        
        return requestSpec;
    }
    
    /**
     * Get endpoint for fetching items based on integration type
     */
    private String getItemsEndpoint(ThirdPartyIntegration.IntegrationType integrationType, String itemType) {
        switch (integrationType) {
            case CONFLUENCE:
                return "/wiki/rest/api/content?limit=100";
            case JIRA:
                return "/rest/api/3/search?maxResults=100";
            case SLACK:
                return "/api/conversations.list?limit=100";
            case GITHUB:
                return "/repos/{owner}/{repo}/contents";
            case GOOGLE_DRIVE:
                return "/drive/v3/files";
            case GOOGLE_DOCS:
                return "/drive/v3/files?q=mimeType='application/vnd.google-apps.document'";
            default:
                return "/items";
        }
    }
    
    /**
     * Parse response from 3rd party API into standardized item format
     */
    private List<Map<String, Object>> parseItemsResponse(ThirdPartyIntegration.IntegrationType integrationType, 
                                                          String itemType, JsonNode jsonResponse) {
        List<Map<String, Object>> items = new ArrayList<>();
        
        try {
            switch (integrationType) {
                case CONFLUENCE:
                    if (jsonResponse.has("results")) {
                        for (JsonNode result : jsonResponse.get("results")) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("id", result.has("id") ? result.get("id").asText() : "");
                            item.put("title", result.has("title") ? result.get("title").asText() : "");
                            item.put("url", result.has("_links") && result.get("_links").has("webui") 
                                ? result.get("_links").get("webui").asText() : "");
                            item.put("type", "PAGE");
                            items.add(item);
                        }
                    }
                    break;
                case JIRA:
                    if (jsonResponse.has("issues")) {
                        for (JsonNode issue : jsonResponse.get("issues")) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("id", issue.has("key") ? issue.get("key").asText() : "");
                            item.put("title", issue.has("fields") && issue.get("fields").has("summary") 
                                ? issue.get("fields").get("summary").asText() : "");
                            item.put("url", "");
                            item.put("type", "ISSUE");
                            items.add(item);
                        }
                    }
                    break;
                default:
                    // Generic parsing
                    if (jsonResponse.isArray()) {
                        for (JsonNode node : jsonResponse) {
                            Map<String, Object> item = new HashMap<>();
                            if (node.has("id")) item.put("id", node.get("id").asText());
                            if (node.has("title")) item.put("title", node.get("title").asText());
                            if (node.has("name")) item.put("title", node.get("name").asText());
                            if (node.has("url")) item.put("url", node.get("url").asText());
                            item.put("type", itemType);
                            items.add(item);
                        }
                    }
            }
        } catch (Exception e) {
            System.err.println("Error parsing items response: " + e.getMessage());
        }
        
        return items;
    }
    
    /**
     * Exchange OAuth authorization code for access token
     */
    public Map<String, Object> exchangeOAuthCode(ThirdPartyIntegration.IntegrationType integrationType,
                                                 String code,
                                                 String clientId,
                                                 String clientSecret,
                                                 String workspaceId,
                                                 String apiEndpoint) {
        String redirectUri = "http://localhost:8080/api/third-party-integrations/oauth/callback";
        String tokenUrl = getTokenUrl(integrationType, workspaceId, apiEndpoint);
        
        WebClient webClient = WebClient.builder().build();
        
        try {
            // Build token request based on service type
            Map<String, String> tokenRequest = buildTokenRequest(integrationType, code, clientId, clientSecret, redirectUri);
            
            String response = webClient.post()
                .uri(tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(buildFormData(tokenRequest))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            Map<String, Object> tokens = new HashMap<>();
            
            // Parse response based on service type
            if (jsonResponse.has("access_token")) {
                tokens.put("access_token", jsonResponse.get("access_token").asText());
            }
            if (jsonResponse.has("refresh_token")) {
                tokens.put("refresh_token", jsonResponse.get("refresh_token").asText());
            }
            if (jsonResponse.has("expires_in")) {
                tokens.put("expires_in", jsonResponse.get("expires_in").asInt());
            }
            
            return tokens;
        } catch (Exception e) {
            System.err.println("Error exchanging OAuth code: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to exchange OAuth code: " + e.getMessage(), e);
        }
    }
    
    private String getTokenUrl(ThirdPartyIntegration.IntegrationType integrationType, String workspaceId, String apiEndpoint) {
        switch (integrationType) {
            case JIRA:
            case CONFLUENCE:
                return "https://auth.atlassian.com/oauth/token";
            case SLACK:
                return "https://slack.com/api/oauth.v2.access";
            case GITHUB:
                return "https://github.com/login/oauth/access_token";
            case GITLAB:
                return "https://gitlab.com/oauth/token";
            case GOOGLE_DRIVE:
            case GOOGLE_DOCS:
            case GOOGLE_SHEETS:
            case GMAIL:
                return "https://oauth2.googleapis.com/token";
            case TEAMS:
            case ONEDRIVE:
            case WORD:
            case EXCEL:
            case OUTLOOK:
                return "https://login.microsoftonline.com/common/oauth2/v2.0/token";
            case SALESFORCE:
            case SERVICE_CLOUD:
                String loginUrl = apiEndpoint != null && apiEndpoint.contains("sandbox") 
                    ? "https://test.salesforce.com" 
                    : "https://login.salesforce.com";
                return loginUrl + "/services/oauth2/token";
            default:
                throw new IllegalArgumentException("Token exchange not supported for: " + integrationType);
        }
    }
    
    private Map<String, String> buildTokenRequest(ThirdPartyIntegration.IntegrationType integrationType,
                                                  String code,
                                                  String clientId,
                                                  String clientSecret,
                                                  String redirectUri) {
        Map<String, String> request = new HashMap<>();
        request.put("code", code);
        request.put("client_id", clientId);
        request.put("client_secret", clientSecret);
        request.put("redirect_uri", redirectUri);
        
        switch (integrationType) {
            case JIRA:
            case CONFLUENCE:
                request.put("grant_type", "authorization_code");
                break;
            case SLACK:
                // Slack uses different format
                break;
            case GITHUB:
                request.put("grant_type", "authorization_code");
                break;
            case GITLAB:
                request.put("grant_type", "authorization_code");
                break;
            case GOOGLE_DRIVE:
            case GOOGLE_DOCS:
            case GOOGLE_SHEETS:
            case GMAIL:
                request.put("grant_type", "authorization_code");
                break;
            case TEAMS:
            case ONEDRIVE:
            case WORD:
            case EXCEL:
            case OUTLOOK:
                request.put("grant_type", "authorization_code");
                break;
            case SALESFORCE:
            case SERVICE_CLOUD:
                request.put("grant_type", "authorization_code");
                break;
        }
        
        return request;
    }
    
    private String buildFormData(Map<String, String> data) {
        return data.entrySet().stream()
            .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(java.util.stream.Collectors.joining("&"));
    }
    
    /**
     * Extract API endpoint from token response (e.g., Slack workspace URL, Jira site)
     */
    public String extractApiEndpointFromTokens(ThirdPartyIntegration.IntegrationType integrationType, 
                                               Map<String, Object> tokens) {
        try {
            switch (integrationType) {
                case SLACK:
                    // Slack token response includes team info
                    if (tokens.containsKey("team")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> team = (Map<String, Object>) tokens.get("team");
                        if (team != null && team.containsKey("id")) {
                            return "https://slack.com/api";
                        }
                    }
                    return "https://slack.com/api";
                    
                case JIRA:
                case CONFLUENCE:
                    // Atlassian tokens include site info in access_token JWT
                    // For now, return default - can be extracted from JWT if needed
                    return "https://api.atlassian.com";
                    
                case GITHUB:
                    return "https://api.github.com";
                    
                case GITLAB:
                    return "https://gitlab.com/api/v4";
                    
                case GOOGLE_DRIVE:
                case GOOGLE_DOCS:
                case GOOGLE_SHEETS:
                case GMAIL:
                    return "https://www.googleapis.com";
                    
                case TEAMS:
                case ONEDRIVE:
                case WORD:
                case EXCEL:
                case OUTLOOK:
                    return "https://graph.microsoft.com";
                    
                case SALESFORCE:
                case SERVICE_CLOUD:
                    // Salesforce instance URL is in token response
                    if (tokens.containsKey("instance_url")) {
                        return (String) tokens.get("instance_url");
                    }
                    return "https://login.salesforce.com";
                    
                default:
                    return "";
            }
        } catch (Exception e) {
            System.err.println("Error extracting API endpoint: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Extract workspace ID from token response
     */
    public String extractWorkspaceIdFromTokens(ThirdPartyIntegration.IntegrationType integrationType,
                                              Map<String, Object> tokens) {
        try {
            switch (integrationType) {
                case SLACK:
                    if (tokens.containsKey("team")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> team = (Map<String, Object>) tokens.get("team");
                        if (team != null && team.containsKey("id")) {
                            return (String) team.get("id");
                        }
                    }
                    return null;
                    
                case JIRA:
                case CONFLUENCE:
                    // Can extract from JWT token if needed
                    return null;
                    
                case SALESFORCE:
                case SERVICE_CLOUD:
                    // Organization ID can be extracted from token
                    return null;
                    
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error extracting workspace ID: " + e.getMessage());
            return null;
        }
    }
}

