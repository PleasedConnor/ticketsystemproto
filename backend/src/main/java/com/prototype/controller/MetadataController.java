package com.prototype.controller;

import com.prototype.entity.MetadataConnection;
import com.prototype.entity.MetadataFieldMapping;
import com.prototype.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metadata")
@CrossOrigin(origins = "*")
public class MetadataController {
    
    @Autowired
    private MetadataService metadataService;
    
    // Connection endpoints
    @GetMapping("/connections")
    public ResponseEntity<List<MetadataConnection>> getAllConnections() {
        return ResponseEntity.ok(metadataService.getAllConnections());
    }
    
    @GetMapping("/connections/active")
    public ResponseEntity<List<MetadataConnection>> getActiveConnections() {
        return ResponseEntity.ok(metadataService.getActiveConnections());
    }
    
    @GetMapping("/connections/{id}")
    public ResponseEntity<MetadataConnection> getConnection(@PathVariable Long id) {
        return ResponseEntity.ok(metadataService.getConnectionById(id));
    }
    
    @PostMapping("/connections")
    public ResponseEntity<MetadataConnection> createConnection(@RequestBody MetadataConnection connection) {
        return ResponseEntity.ok(metadataService.createConnection(connection));
    }
    
    @PutMapping("/connections/{id}")
    public ResponseEntity<MetadataConnection> updateConnection(@PathVariable Long id, @RequestBody MetadataConnection connection) {
        return ResponseEntity.ok(metadataService.updateConnection(id, connection));
    }
    
    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        metadataService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/connections/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        MetadataConnection connection = metadataService.getConnectionById(id);
        return ResponseEntity.ok(metadataService.testConnection(connection));
    }
    
    // Mapping endpoints
    @GetMapping("/connections/{connectionId}/mappings")
    public ResponseEntity<List<MetadataFieldMapping>> getMappings(@PathVariable Long connectionId) {
        return ResponseEntity.ok(metadataService.getMappingsByConnection(connectionId));
    }
    
    @GetMapping("/connections/{connectionId}/mappings/active")
    public ResponseEntity<List<MetadataFieldMapping>> getActiveMappings(@PathVariable Long connectionId) {
        return ResponseEntity.ok(metadataService.getActiveMappingsByConnection(connectionId));
    }
    
    @PostMapping("/connections/{connectionId}/mappings")
    public ResponseEntity<List<MetadataFieldMapping>> saveMappings(
            @PathVariable Long connectionId,
            @RequestBody List<MetadataFieldMapping> mappings) {
        metadataService.saveMappings(connectionId, mappings);
        return ResponseEntity.ok(metadataService.getMappingsByConnection(connectionId));
    }
    
    @PostMapping("/mappings")
    public ResponseEntity<MetadataFieldMapping> createMapping(@RequestBody MetadataFieldMapping mapping) {
        return ResponseEntity.ok(metadataService.createMapping(mapping));
    }
    
    @PutMapping("/mappings/{id}")
    public ResponseEntity<MetadataFieldMapping> updateMapping(@PathVariable Long id, @RequestBody MetadataFieldMapping mapping) {
        return ResponseEntity.ok(metadataService.updateMapping(id, mapping));
    }
    
    @DeleteMapping("/mappings/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        metadataService.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }
    
    // AI Access endpoints
    @GetMapping("/mappings/ai-access")
    public ResponseEntity<List<MetadataFieldMapping>> getAllMappingsForAIAccess() {
        try {
            List<MetadataFieldMapping> mappings = metadataService.getAllMappingsForAIAccess();
            // Ensure connection is loaded for each mapping
            mappings.forEach(m -> {
                if (m.getConnection() != null) {
                    // Access connection to force eager loading
                    m.getConnection().getName();
                }
            });
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @PutMapping("/mappings/{id}/ai-access")
    public ResponseEntity<MetadataFieldMapping> updateAIAccess(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        Boolean aiAccessible = request.get("aiAccessible");
        if (aiAccessible == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(metadataService.updateAIAccess(id, aiAccessible));
    }
    
    @PutMapping("/mappings/ai-access/bulk")
    public ResponseEntity<List<MetadataFieldMapping>> bulkUpdateAIAccess(
            @RequestBody Map<Long, Boolean> accessMap) {
        return ResponseEntity.ok(metadataService.bulkUpdateAIAccess(accessMap));
    }
    
    @GetMapping("/mappings/{mappingId}/test-value")
    public ResponseEntity<Map<String, Object>> testMappingValue(
            @PathVariable Long mappingId,
            @RequestParam(required = false) Long ticketId) {
        try {
            Map<String, Object> result = metadataService.testMappingValue(mappingId, ticketId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get available metadata variables (not already in use)
     * @param excludeVariable Optional variable to exclude from "in use" check (for editing)
     */
    @GetMapping("/variables/available")
    public ResponseEntity<List<String>> getAvailableVariables(
            @RequestParam(required = false) String excludeVariable) {
        return ResponseEntity.ok(metadataService.getAvailableMetadataVariables(excludeVariable));
    }
}

