package com.prototype.controller;

import com.prototype.entity.CustomerProfileField;
import com.prototype.entity.Ticket;
import com.prototype.repository.TicketRepository;
import com.prototype.service.CustomerProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customer-profile")
@CrossOrigin(origins = "*")
public class CustomerProfileController {
    
    @Autowired
    private CustomerProfileService profileService;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    /**
     * Get all visible customer profile fields
     */
    @GetMapping("/fields")
    public ResponseEntity<List<CustomerProfileField>> getAllFields() {
        return ResponseEntity.ok(profileService.getAllVisibleFields());
    }
    
    /**
     * Get all customer profile fields (including hidden) - for configuration
     */
    @GetMapping("/fields/all")
    public ResponseEntity<List<CustomerProfileField>> getAllFieldsIncludingHidden() {
        return ResponseEntity.ok(profileService.getAllFields());
    }
    
    /**
     * Get customer profile data for a specific ticket
     * Resolves metadata variables to actual values
     */
    @GetMapping("/data/{ticketId}")
    public ResponseEntity<Map<String, Object>> getCustomerProfileData(@PathVariable Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));
        
        Map<String, Object> profileData = profileService.getCustomerProfileData(ticket);
        return ResponseEntity.ok(profileData);
    }
    
    /**
     * Create a new customer profile field
     */
    @PostMapping("/fields")
    public ResponseEntity<CustomerProfileField> createField(@RequestBody CustomerProfileField field) {
        return ResponseEntity.ok(profileService.saveField(field));
    }
    
    /**
     * Update an existing customer profile field
     */
    @PutMapping("/fields/{id}")
    public ResponseEntity<CustomerProfileField> updateField(
            @PathVariable Long id,
            @RequestBody CustomerProfileField field) {
        CustomerProfileField existing = profileService.getFieldById(id);
        existing.setFieldLabel(field.getFieldLabel());
        existing.setFieldKey(field.getFieldKey());
        existing.setMetadataVariable(field.getMetadataVariable());
        existing.setFieldType(field.getFieldType());
        existing.setDisplayOrder(field.getDisplayOrder());
        existing.setIsVisible(field.getIsVisible());
        existing.setIsEditable(field.getIsEditable());
        existing.setIcon(field.getIcon());
        existing.setDescription(field.getDescription());
        return ResponseEntity.ok(profileService.saveField(existing));
    }
    
    /**
     * Delete a customer profile field
     */
    @DeleteMapping("/fields/{id}")
    public ResponseEntity<Void> deleteField(@PathVariable Long id) {
        profileService.deleteField(id);
        return ResponseEntity.noContent().build();
    }
}

