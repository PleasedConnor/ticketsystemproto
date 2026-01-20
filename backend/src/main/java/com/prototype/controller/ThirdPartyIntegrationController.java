package com.prototype.controller;

import com.prototype.entity.BlacklistedItem;
import com.prototype.entity.ThirdPartyIntegration;
import com.prototype.service.OAuthService;
import com.prototype.service.ThirdPartyIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/third-party-integrations")
@CrossOrigin(origins = "*")
public class ThirdPartyIntegrationController {
    
    @Autowired
    private ThirdPartyIntegrationService integrationService;
    
    @Autowired
    private OAuthService oAuthService;
    
    @GetMapping
    public ResponseEntity<List<ThirdPartyIntegration>> getAllIntegrations() {
        try {
            List<ThirdPartyIntegration> integrations = integrationService.getAllIntegrations();
            return ResponseEntity.ok(integrations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<ThirdPartyIntegration>> getActiveIntegrations() {
        try {
            List<ThirdPartyIntegration> integrations = integrationService.getActiveIntegrations();
            return ResponseEntity.ok(integrations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ThirdPartyIntegration> getIntegrationById(@PathVariable Long id) {
        try {
            ThirdPartyIntegration integration = integrationService.getIntegrationById(id);
            return ResponseEntity.ok(integration);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<ThirdPartyIntegration> createIntegration(@RequestBody ThirdPartyIntegration integration) {
        try {
            ThirdPartyIntegration created = integrationService.createIntegration(integration);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ThirdPartyIntegration> updateIntegration(@PathVariable Long id, 
                                                                   @RequestBody ThirdPartyIntegration integration) {
        try {
            ThirdPartyIntegration updated = integrationService.updateIntegration(id, integration);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntegration(@PathVariable Long id) {
        try {
            integrationService.deleteIntegration(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{id}/ai-access")
    public ResponseEntity<Void> updateAIAccess(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        try {
            Boolean aiAccessible = request.get("aiAccessible");
            integrationService.updateAIAccess(id, aiAccessible);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{id}/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        try {
            ThirdPartyIntegration integration = integrationService.getIntegrationById(id);
            Map<String, Object> result = integrationService.testConnection(integration);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}/items")
    public ResponseEntity<List<Map<String, Object>>> fetchAvailableItems(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "PAGE") String itemType) {
        try {
            ThirdPartyIntegration integration = integrationService.getIntegrationById(id);
            List<Map<String, Object>> items = integrationService.fetchAvailableItems(integration, itemType);
            return ResponseEntity.ok(items);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}/blacklist")
    public ResponseEntity<List<BlacklistedItem>> getBlacklistedItems(@PathVariable Long id) {
        try {
            List<BlacklistedItem> items = integrationService.getBlacklistedItems(id);
            return ResponseEntity.ok(items);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{id}/blacklist")
    public ResponseEntity<BlacklistedItem> addToBlacklist(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            BlacklistedItem item = integrationService.addToBlacklist(
                id,
                request.get("itemType"),
                request.get("itemId"),
                request.get("itemTitle"),
                request.get("itemUrl"),
                request.get("reason")
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{id}/blacklist/bulk")
    public ResponseEntity<List<BlacklistedItem>> bulkAddToBlacklist(
            @PathVariable Long id,
            @RequestBody List<Map<String, String>> items) {
        try {
            List<BlacklistedItem> blacklistedItems = integrationService.bulkAddToBlacklist(id, items);
            return ResponseEntity.status(HttpStatus.CREATED).body(blacklistedItems);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{id}/blacklist/{itemId}")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable Long id, @PathVariable String itemId) {
        try {
            integrationService.removeFromBlacklist(id, itemId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get list of available integrations
     */
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableIntegrations() {
        try {
            Map<String, Object> integrations = com.prototype.config.IntegrationConfig.getAvailableIntegrations();
            return ResponseEntity.ok(integrations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Check if OAuth app credentials are configured for an integration type
     * Returns the first configured integration's credentials (for reuse)
     */
    @GetMapping("/oauth-config/{integrationType}")
    public ResponseEntity<Map<String, Object>> getOAuthAppConfig(@PathVariable String integrationType) {
        try {
            ThirdPartyIntegration.IntegrationType type = 
                ThirdPartyIntegration.IntegrationType.valueOf(integrationType.toUpperCase());
            
            List<ThirdPartyIntegration> configs = integrationService.findOAuthAppConfigsByType(type);
            
            if (configs.isEmpty()) {
                return ResponseEntity.ok(Map.of("configured", false));
            }
            
            // Return the most recently updated config
            ThirdPartyIntegration config = configs.get(0);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("configured", true);
            result.put("clientId", config.getClientId());
            result.put("apiEndpoint", config.getApiEndpoint() != null ? config.getApiEndpoint() : "");
            result.put("workspaceId", config.getWorkspaceId() != null ? config.getWorkspaceId() : "");
            // Don't return clientSecret for security - it will be retrieved from DB during token exchange
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("configured", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Generate OAuth URL for connecting a new integration
     * Client ID and endpoint are retrieved from environment/config
     */
    @PostMapping("/oauth/authorize")
    public ResponseEntity<Map<String, String>> generateOAuthUrl(@RequestBody Map<String, String> request) {
        try {
            ThirdPartyIntegration.IntegrationType integrationType = 
                ThirdPartyIntegration.IntegrationType.valueOf(request.get("integrationType"));
            
            // Get client ID from request (frontend provides it)
            String clientId = request.get("clientId");
            
            // If not provided, try environment variable (for backward compatibility)
            if (clientId == null || clientId.isEmpty()) {
                clientId = System.getenv("OAUTH_CLIENT_ID_" + integrationType.name());
            }
            
            // If still not found, return error asking user to provide it
            if (clientId == null || clientId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Client ID is required. Please enter your OAuth Client ID in the configuration dialog."));
            }
            
            String workspaceId = request.get("workspaceId");
            String state = oAuthService.generateState();
            
            // Store integration type in state for callback
            Map<String, String> result = oAuthService.generateOAuthUrl(integrationType, clientId, workspaceId, state);
            result.put("integrationType", integrationType.name());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to generate OAuth URL: " + e.getMessage()));
        }
    }
    
    /**
     * OAuth callback endpoint - handles redirect from 3rd party service
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<String> oauthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        
        try {
            if (error != null) {
                // OAuth error occurred
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/ai-data-access?oauth_error=" + error)
                    .body("OAuth error: " + error);
            }
            
            if (code == null || state == null) {
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/ai-data-access?oauth_error=missing_parameters")
                    .body("Missing OAuth parameters");
            }
            
            // Exchange code for tokens (this will be handled by the service)
            // For now, redirect to frontend with code and state
            String redirectUrl = String.format(
                "http://localhost:3000/ai-data-access?oauth_code=%s&oauth_state=%s",
                code, state
            );
            
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .body("Redirecting...");
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "http://localhost:3000/ai-data-access?oauth_error=server_error")
                .body("Server error during OAuth callback");
        }
    }
    
    /**
     * Exchange OAuth code for access token
     * Client ID and Secret are retrieved from environment variables
     */
    @PostMapping("/oauth/token")
    public ResponseEntity<Map<String, Object>> exchangeToken(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            // State is used for verification - verify it matches stored state
            @SuppressWarnings("unused")
            String state = request.get("state");
            ThirdPartyIntegration.IntegrationType integrationType = 
                ThirdPartyIntegration.IntegrationType.valueOf(request.get("integrationType"));
            String name = request.get("name");
            
            // Get API endpoint and workspace ID from request (frontend may provide them)
            String apiEndpoint = request.get("apiEndpoint");
            String workspaceId = request.get("workspaceId");
            
            // Get client credentials - try multiple sources in order:
            // 1. From request (if provided)
            // 2. From existing integration with same type (reuse OAuth app config)
            // 3. From environment variables
            String clientId = request.get("clientId");
            String clientSecret = request.get("clientSecret");
            
            if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
                // Try to find existing OAuth app config for this integration type
                List<ThirdPartyIntegration> existingConfigs = integrationService.findOAuthAppConfigsByType(integrationType);
                if (!existingConfigs.isEmpty()) {
                    ThirdPartyIntegration existingConfig = existingConfigs.get(0);
                    if (clientId == null || clientId.isEmpty()) {
                        clientId = existingConfig.getClientId();
                    }
                    if (clientSecret == null || clientSecret.isEmpty()) {
                        clientSecret = existingConfig.getClientSecret();
                    }
                    // Also use existing API endpoint and workspace ID if not provided
                    if ((apiEndpoint == null || apiEndpoint.isEmpty()) && existingConfig.getApiEndpoint() != null) {
                        apiEndpoint = existingConfig.getApiEndpoint();
                    }
                    if ((workspaceId == null || workspaceId.isEmpty()) && existingConfig.getWorkspaceId() != null) {
                        workspaceId = existingConfig.getWorkspaceId();
                    }
                }
            }
            
            // Fallback to environment variables if still not found
            if (clientId == null || clientId.isEmpty()) {
                clientId = System.getenv("OAUTH_CLIENT_ID_" + integrationType.name());
            }
            if (clientSecret == null || clientSecret.isEmpty()) {
                clientSecret = System.getenv("OAUTH_CLIENT_SECRET_" + integrationType.name());
            }
            
            if (clientId == null || clientSecret == null || clientId.isEmpty() || clientSecret.isEmpty()) {
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("success", false);
                result.put("error", "OAuth credentials not configured. Please set up OAuth app credentials first.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            // Exchange code for tokens
            Map<String, Object> tokens = integrationService.exchangeOAuthCode(
                integrationType, code, clientId, clientSecret, workspaceId, apiEndpoint
            );
            
            // Extract workspace/instance info from token response if not provided
            if (apiEndpoint == null || apiEndpoint.isEmpty()) {
                apiEndpoint = integrationService.extractApiEndpointFromTokens(integrationType, tokens);
            }
            if (workspaceId == null || workspaceId.isEmpty()) {
                workspaceId = integrationService.extractWorkspaceIdFromTokens(integrationType, tokens);
            }
            
            // Create integration with tokens
            ThirdPartyIntegration integration = new ThirdPartyIntegration();
            integration.setName(name != null ? name : integrationType.name() + " Integration");
            integration.setIntegrationType(integrationType);
            integration.setAuthType("OAUTH2");
            integration.setClientId(clientId);
            integration.setClientSecret(clientSecret);
            integration.setApiEndpoint(apiEndpoint);
            integration.setWorkspaceId(workspaceId);
            integration.setAccessToken((String) tokens.get("access_token"));
            integration.setRefreshToken((String) tokens.get("refresh_token"));
            integration.setIsActive(true);
            integration.setAiAccessible(true);
            integration.setSyncEnabled(true);
            
            ThirdPartyIntegration created = integrationService.createIntegration(integration);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", true);
            result.put("integration", created);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}

