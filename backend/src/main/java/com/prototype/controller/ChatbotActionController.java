package com.prototype.controller;

import com.prototype.entity.ChatbotAction;
import com.prototype.service.ChatbotActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chatbot-actions")
@CrossOrigin(origins = "*")
public class ChatbotActionController {
    
    @Autowired
    private ChatbotActionService actionService;
    
    @GetMapping
    public ResponseEntity<List<ChatbotAction>> getAllActions() {
        try {
            List<ChatbotAction> actions = actionService.getAllActions();
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<ChatbotAction>> getActiveActions() {
        try {
            List<ChatbotAction> actions = actionService.getActiveActions();
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ChatbotAction> getActionById(@PathVariable Long id) {
        try {
            ChatbotAction action = actionService.getActionById(id);
            return ResponseEntity.ok(action);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createAction(@RequestBody ChatbotAction action) {
        try {
            System.out.println("=== CREATE ACTION REQUEST ===");
            System.out.println("Name: " + action.getName());
            System.out.println("Action Key: " + action.getActionKey());
            System.out.println("Component Type: " + action.getComponentType());
            System.out.println("Config: " + (action.getConfig() != null ? action.getConfig().substring(0, Math.min(200, action.getConfig().length())) : "null"));
            System.out.println("Is Active: " + action.getIsActive());
            System.out.println("=== END REQUEST ===");
            
            ChatbotAction created = actionService.createAction(action);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            System.err.println("=== ERROR CREATING ACTION ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAction(@PathVariable Long id, @RequestBody ChatbotAction action) {
        try {
            System.out.println("=== UPDATE ACTION REQUEST ===");
            System.out.println("ID: " + id);
            System.out.println("Name: " + action.getName());
            System.out.println("Action Key: " + action.getActionKey());
            System.out.println("Component Type: " + action.getComponentType());
            System.out.println("Config: " + (action.getConfig() != null ? action.getConfig().substring(0, Math.min(200, action.getConfig().length())) : "null"));
            System.out.println("=== END REQUEST ===");
            
            ChatbotAction updated = actionService.updateAction(id, action);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            System.err.println("=== ERROR UPDATING ACTION ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAction(@PathVariable Long id) {
        try {
            actionService.deleteAction(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

