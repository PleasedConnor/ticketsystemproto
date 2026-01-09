package com.prototype.controller;

import com.prototype.entity.AIConfiguration;
import com.prototype.service.AIConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-configuration")
@CrossOrigin(origins = "*")
public class AIConfigurationController {
    
    @Autowired
    private AIConfigurationService configurationService;
    
    /**
     * Get the main AI configuration
     */
    @GetMapping
    public ResponseEntity<AIConfiguration> getMainConfiguration() {
        return ResponseEntity.ok(configurationService.getMainConfiguration());
    }
    
    /**
     * Update the main AI configuration
     */
    @PutMapping
    public ResponseEntity<AIConfiguration> updateConfiguration(
            @RequestBody AIConfiguration configuration) {
        return ResponseEntity.ok(configurationService.saveConfiguration(configuration));
    }
}

