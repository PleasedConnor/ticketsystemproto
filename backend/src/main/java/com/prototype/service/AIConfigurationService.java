package com.prototype.service;

import com.prototype.entity.AIConfiguration;
import com.prototype.entity.Ticket;
import com.prototype.entity.User;
import com.prototype.repository.AIConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIConfigurationService {
    
    @Autowired
    private AIConfigurationRepository configurationRepository;
    
    // Pattern for template variables like {{user.name}}, {{user.location}}, etc.
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * Get the main AI configuration (there should only be one)
     */
    public AIConfiguration getMainConfiguration() {
        List<AIConfiguration> configs = configurationRepository.findAll();
        if (configs.isEmpty()) {
            // Create a default empty configuration
            AIConfiguration defaultConfig = new AIConfiguration();
            return configurationRepository.save(defaultConfig);
        }
        // Return the first one (or we could enforce only one exists)
        return configs.get(0);
    }
    
    /**
     * Get all configurations (for compatibility)
     */
    public List<AIConfiguration> getAllConfigurations() {
        return configurationRepository.findAll();
    }
    
    /**
     * Get all configurations formatted as a prompt with priority hierarchy
     * Priority: Edge Case Rules > Escalation Processes > General Rules
     */
    public String getAllActiveConfigurationsAsPrompt() {
        AIConfiguration config = getMainConfiguration();
        return buildPromptFromConfiguration(config, null);
    }
    
    /**
     * Get configurations relevant to a specific ticket context with template variable replacement
     */
    public String getRelevantConfigurationsAsPrompt(Long ticketId, String ticketCategory, 
                                                     String ticketPriority, String customerMessage) {
        AIConfiguration config = getMainConfiguration();
        // Ticket will be passed from AIService for template variable replacement
        return buildPromptFromConfiguration(config, null);
    }
    
    /**
     * Get configurations with ticket for template variable replacement
     */
    public String getRelevantConfigurationsAsPrompt(Ticket ticket) {
        AIConfiguration config = getMainConfiguration();
        return buildPromptFromConfiguration(config, ticket);
    }
    
    /**
     * Build prompt from configuration with priority hierarchy
     */
    private String buildPromptFromConfiguration(AIConfiguration config, Ticket ticket) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("=== AI CUSTOMER SERVICE GUIDELINES ===\n\n");
        
        // 1. General AI Rules (lowest priority, always present)
        if (config.getGeneralRules() != null && !config.getGeneralRules().trim().isEmpty()) {
            prompt.append("=== GENERAL GUIDELINES ===\n");
            prompt.append("These guidelines provide the foundation for all interactions. Use them to inform your approach.\n\n");
            String generalRules = replaceTemplateVariables(config.getGeneralRules(), ticket);
            prompt.append(generalRules).append("\n\n");
        }
        
        // 2. Escalation Processes (overrides General Rules)
        if (config.getEscalationProcesses() != null && !config.getEscalationProcesses().trim().isEmpty()) {
            prompt.append("=== ESCALATION GUIDELINES ===\n");
            prompt.append("When escalation is needed, these guidelines take precedence over general guidelines.\n\n");
            String escalation = replaceTemplateVariables(config.getEscalationProcesses(), ticket);
            prompt.append(escalation).append("\n\n");
        }
        
        // 3. Edge Case AI Rules (overrides both)
        if (config.getEdgeCaseRules() != null && !config.getEdgeCaseRules().trim().isEmpty()) {
            prompt.append("=== EDGE CASE GUIDELINES ===\n");
            prompt.append("For specific edge cases, these guidelines take highest priority.\n\n");
            String edgeCases = replaceTemplateVariables(config.getEdgeCaseRules(), ticket);
            prompt.append(edgeCases).append("\n\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * Replace template variables in configuration text
     * Supported variables: {{user.name}}, {{user.location}}, {{user.email}}, {{ticket.id}}, {{ticket.status}}, etc.
     * If a variable cannot be resolved (empty or no context), it's replaced with an empty string.
     * This removes the {{variable}} syntax completely, allowing the configuration's explicit instructions
     * (e.g., "ask for the customer's name if not available") to guide the AI behavior.
     */
    public String replaceTemplateVariables(String text, Ticket ticket) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        Matcher matcher = TEMPLATE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String variable = matcher.group(1).trim();
            String replacement = getVariableValue(variable, ticket);
            // If replacement is empty, replace with empty string (removes the {{variable}} syntax)
            // This prevents the AI from seeing literal {{user.name}} in the prompt
            replacement = replacement.replace("$", "\\$");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Get the value for a template variable
     * Returns the actual value if available, or empty string if not available.
     * Empty string removes the {{variable}} syntax, allowing configuration instructions
     * (like "ask for information if not available") to guide the AI.
     */
    private String getVariableValue(String variable, Ticket ticket) {
        if (variable == null || variable.trim().isEmpty()) {
            return "";
        }
        
        String[] parts = variable.split("\\.");
        if (parts.length < 2) {
            return ""; // Return empty if invalid format (don't pass through literal syntax)
        }
        
        String entity = parts[0].toLowerCase();
        String field = parts[1].toLowerCase();
        
        if ("user".equals(entity)) {
            if (ticket != null && ticket.getUser() != null) {
                User user = ticket.getUser();
                switch (field) {
                    case "name":
                        String name = user.getName();
                        // Return name if available, otherwise empty string (variable will be removed)
                        // The configuration text itself contains instructions to ask if needed
                        return (name != null && !name.trim().isEmpty()) ? name : "";
                    case "location":
                        return user.getLocation() != null && !user.getLocation().trim().isEmpty() ? user.getLocation() : "";
                    case "email":
                        return user.getEmail() != null && !user.getEmail().trim().isEmpty() ? user.getEmail() : "";
                    case "device":
                        return user.getDevice() != null && !user.getDevice().trim().isEmpty() ? user.getDevice() : "";
                    default:
                        return ""; // Unknown user field, return empty
                }
            } else {
                // No ticket or user available - return empty string (variable will be removed)
                // The configuration text itself contains instructions to ask if needed
                return "";
            }
        } else if ("ticket".equals(entity)) {
            if (ticket != null) {
                switch (field) {
                    case "id":
                        return ticket.getId() != null ? ticket.getId().toString() : "";
                    case "status":
                        return ticket.getStatus() != null ? ticket.getStatus().toString() : "";
                    case "priority":
                        return ticket.getPriority() != null ? ticket.getPriority().toString() : "";
                    case "category":
                        return ticket.getCategory() != null ? ticket.getCategory().toString() : "";
                    case "subject":
                        return ticket.getSubject() != null ? ticket.getSubject() : "";
                    default:
                        return ""; // Unknown ticket field, return empty
                }
            } else {
                // No ticket available - return empty string
                return "";
            }
        }
        
        // Unknown variable type, return empty string
        return "";
    }
    
    
    /**
     * Save or update the main configuration
     */
    public AIConfiguration saveConfiguration(AIConfiguration configuration) {
        if (configuration.getId() == null) {
            // If no ID, get or create the main configuration
            AIConfiguration existing = getMainConfiguration();
            configuration.setId(existing.getId());
            configuration.setCreatedAt(existing.getCreatedAt());
        }
        configuration.setUpdatedAt(java.time.LocalDateTime.now());
        return configurationRepository.save(configuration);
    }
    
    /**
     * Get configuration by ID
     */
    public AIConfiguration getConfigurationById(Long id) {
        return configurationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Configuration not found with id: " + id));
    }
}

