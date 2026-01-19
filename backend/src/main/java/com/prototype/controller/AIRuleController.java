package com.prototype.controller;

import com.prototype.entity.AIRule;
import com.prototype.service.AIRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/rules")
@CrossOrigin(origins = "*")
public class AIRuleController {
    
    @Autowired
    private AIRuleService airuleService;
    
    @GetMapping
    public ResponseEntity<List<AIRule>> getAllRules() {
        return ResponseEntity.ok(airuleService.getAllRules());
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<AIRule>> getRulesByCategory(@PathVariable String category) {
        try {
            AIRule.RuleCategory ruleCategory = AIRule.RuleCategory.valueOf(category.toUpperCase());
            return ResponseEntity.ok(airuleService.getRulesByCategory(ruleCategory));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AIRule> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(airuleService.getRuleById(id));
    }
    
    @PostMapping
    public ResponseEntity<AIRule> createRule(@RequestBody AIRule rule) {
        return ResponseEntity.ok(airuleService.createRule(rule));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AIRule> updateRule(@PathVariable Long id, @RequestBody AIRule rule) {
        rule.setId(id);
        return ResponseEntity.ok(airuleService.updateRule(rule));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        airuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/reorder")
    public ResponseEntity<Void> reorderRules(@RequestBody List<AIRule> rules) {
        airuleService.reorderRules(rules);
        return ResponseEntity.ok().build();
    }
}

