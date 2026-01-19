package com.prototype.service;

import com.prototype.entity.AIRule;
import com.prototype.entity.Ticket;
import com.prototype.repository.AIRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AIRuleService {
    
    @Autowired
    private AIRuleRepository ruleRepository;
    
    @Autowired
    private AIConfigurationService configurationService;
    
    public List<AIRule> getAllRules() {
        return ruleRepository.findAllByOrderByCategoryAscDisplayOrderAsc();
    }
    
    public List<AIRule> getRulesByCategory(AIRule.RuleCategory category) {
        return ruleRepository.findByCategoryOrderByDisplayOrderAsc(category);
    }
    
    public AIRule getRuleById(Long id) {
        return ruleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found with id: " + id));
    }
    
    @Transactional
    public AIRule createRule(AIRule rule) {
        // Set display order if not provided
        if (rule.getDisplayOrder() == null) {
            List<AIRule> existingRules = getRulesByCategory(rule.getCategory());
            int maxOrder = existingRules.stream()
                .mapToInt(r -> r.getDisplayOrder() != null ? r.getDisplayOrder() : 0)
                .max()
                .orElse(-1);
            rule.setDisplayOrder(maxOrder + 1);
        }
        return ruleRepository.save(rule);
    }
    
    @Transactional
    public AIRule updateRule(AIRule rule) {
        AIRule existing = getRuleById(rule.getId());
        existing.setTitle(rule.getTitle());
        existing.setContent(rule.getContent());
        existing.setCategory(rule.getCategory());
        if (rule.getDisplayOrder() != null) {
            existing.setDisplayOrder(rule.getDisplayOrder());
        }
        return ruleRepository.save(existing);
    }
    
    @Transactional
    public void deleteRule(Long id) {
        AIRule rule = getRuleById(id);
        AIRule.RuleCategory category = rule.getCategory();
        ruleRepository.deleteById(id);
        
        // Reorder remaining rules in the same category
        List<AIRule> remainingRules = getRulesByCategory(category);
        for (int i = 0; i < remainingRules.size(); i++) {
            remainingRules.get(i).setDisplayOrder(i);
            ruleRepository.save(remainingRules.get(i));
        }
    }
    
    @Transactional
    public void reorderRules(List<AIRule> rules) {
        for (AIRule rule : rules) {
            AIRule existing = getRuleById(rule.getId());
            existing.setDisplayOrder(rule.getDisplayOrder());
            ruleRepository.save(existing);
        }
    }
    
    /**
     * Get all rules formatted as a prompt with priority hierarchy
     * Priority: ADDITIONAL > ESCALATION > GENERAL
     */
    public String getAllRulesAsPrompt(Ticket ticket) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("=== AI CUSTOMER SERVICE GUIDELINES ===\n\n");
        
        // 1. General AI Rules (lowest priority)
        List<AIRule> generalRules = getRulesByCategory(AIRule.RuleCategory.GENERAL);
        if (!generalRules.isEmpty()) {
            prompt.append("=== GENERAL GUIDELINES ===\n");
            prompt.append("These guidelines provide the foundation for all interactions. Use them to inform your approach.\n\n");
            for (AIRule rule : generalRules) {
                prompt.append("[").append(rule.getTitle()).append("]\n");
                String content = configurationService.replaceTemplateVariables(rule.getContent(), ticket);
                prompt.append(content).append("\n\n");
            }
        }
        
        // 2. Escalation Rules (medium priority)
        List<AIRule> escalationRules = getRulesByCategory(AIRule.RuleCategory.ESCALATION);
        if (!escalationRules.isEmpty()) {
            prompt.append("=== ESCALATION GUIDELINES ===\n");
            prompt.append("When escalation is needed, these guidelines take precedence over general guidelines.\n\n");
            for (AIRule rule : escalationRules) {
                prompt.append("[").append(rule.getTitle()).append("]\n");
                String content = configurationService.replaceTemplateVariables(rule.getContent(), ticket);
                prompt.append(content).append("\n\n");
            }
        }
        
        // 3. Additional AI Rules (highest priority)
        List<AIRule> additionalRules = getRulesByCategory(AIRule.RuleCategory.ADDITIONAL);
        if (!additionalRules.isEmpty()) {
            prompt.append("=== ADDITIONAL GUIDELINES ===\n");
            prompt.append("For specific scenarios, these guidelines take highest priority.\n\n");
            for (AIRule rule : additionalRules) {
                prompt.append("[").append(rule.getTitle()).append("]\n");
                String content = configurationService.replaceTemplateVariables(rule.getContent(), ticket);
                prompt.append(content).append("\n\n");
            }
        }
        
        return prompt.toString();
    }
}

