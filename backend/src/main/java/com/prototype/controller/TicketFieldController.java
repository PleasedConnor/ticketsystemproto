package com.prototype.controller;

import com.prototype.entity.Ticket;
import com.prototype.entity.TicketField;
import com.prototype.repository.TicketRepository;
import com.prototype.service.TicketFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ticket-fields")
@CrossOrigin(origins = "*")
public class TicketFieldController {
    
    @Autowired
    private TicketFieldService ticketFieldService;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    /**
     * Get all visible ticket fields
     */
    @GetMapping("/fields")
    public ResponseEntity<List<TicketField>> getAllFields() {
        return ResponseEntity.ok(ticketFieldService.getAllVisibleFields());
    }
    
    /**
     * Get all ticket fields (including hidden) - for configuration
     */
    @GetMapping("/fields/all")
    public ResponseEntity<List<TicketField>> getAllFieldsIncludingHidden() {
        return ResponseEntity.ok(ticketFieldService.getAllFields());
    }
    
    /**
     * Get ticket fields for a specific location
     */
    @GetMapping("/fields/location/{location}")
    public ResponseEntity<List<TicketField>> getFieldsByLocation(@PathVariable String location) {
        return ResponseEntity.ok(ticketFieldService.getFieldsByLocation(location));
    }
    
    /**
     * Get ticket field data for a specific ticket and location
     * Resolves metadata variables to actual values
     */
    @GetMapping("/data/{ticketId}/{location}")
    public ResponseEntity<Map<String, Object>> getTicketFieldData(
            @PathVariable Long ticketId,
            @PathVariable String location) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));
        
        Map<String, Object> fieldData = ticketFieldService.getTicketFieldData(ticket, location);
        return ResponseEntity.ok(fieldData);
    }
    
    /**
     * Create a new ticket field
     */
    @PostMapping("/fields")
    public ResponseEntity<TicketField> createField(@RequestBody TicketField field) {
        // Display location is always CUSTOMER_INFO for ticket fields
        if (field.getDisplayLocation() == null || field.getDisplayLocation().trim().isEmpty()) {
            field.setDisplayLocation("CUSTOMER_INFO");
        }
        return ResponseEntity.ok(ticketFieldService.saveField(field));
    }
    
    /**
     * Update an existing ticket field
     */
    @PutMapping("/fields/{id}")
    public ResponseEntity<TicketField> updateField(
            @PathVariable Long id,
            @RequestBody TicketField field) {
        TicketField existing = ticketFieldService.getFieldById(id);
        existing.setFieldLabel(field.getFieldLabel());
        existing.setFieldKey(field.getFieldKey());
        existing.setMetadataVariable(field.getMetadataVariable());
        existing.setFieldType(field.getFieldType());
        // Display location is always CUSTOMER_INFO for ticket fields
        if (field.getDisplayLocation() == null || field.getDisplayLocation().trim().isEmpty()) {
            existing.setDisplayLocation("CUSTOMER_INFO");
        } else {
            existing.setDisplayLocation(field.getDisplayLocation());
        }
        existing.setDisplayOrder(field.getDisplayOrder());
        existing.setIsVisible(field.getIsVisible());
        existing.setIsEditable(field.getIsEditable());
        existing.setIcon(field.getIcon());
        existing.setDescription(field.getDescription());
        return ResponseEntity.ok(ticketFieldService.saveField(existing));
    }
    
    /**
     * Delete a ticket field
     */
    @DeleteMapping("/fields/{id}")
    public ResponseEntity<Void> deleteField(@PathVariable Long id) {
        ticketFieldService.deleteField(id);
        return ResponseEntity.noContent().build();
    }
}

