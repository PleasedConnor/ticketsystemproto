package com.prototype.service;

import com.prototype.entity.Ticket;
import com.prototype.entity.TicketField;
import com.prototype.repository.TicketFieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing ticket fields and resolving their values from metadata
 */
@Service
public class TicketFieldService {
    
    @Autowired
    private TicketFieldRepository ticketFieldRepository;
    
    @Autowired(required = false)
    private MetadataService metadataService;
    
    /**
     * Get all visible ticket fields ordered by location and display order
     */
    @Transactional(readOnly = true)
    public List<TicketField> getAllVisibleFields() {
        return ticketFieldRepository.findAllVisibleOrdered();
    }
    
    /**
     * Get all ticket fields (including hidden)
     */
    @Transactional(readOnly = true)
    public List<TicketField> getAllFields() {
        return ticketFieldRepository.findAll();
    }
    
    /**
     * Get ticket fields for a specific display location
     */
    @Transactional(readOnly = true)
    public List<TicketField> getFieldsByLocation(String location) {
        return ticketFieldRepository.findByLocationAndVisible(location);
    }
    
    /**
     * Get ticket field data with resolved values for a specific ticket
     * Resolves metadata variables to actual values
     * Priority: 1. User entity data (for standard fields), 2. Metadata mappings (external API)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTicketFieldData(Ticket ticket, String location) {
        List<TicketField> fields = ticketFieldRepository.findByLocationAndVisible(location);
        Map<String, Object> fieldData = new HashMap<>();
        
        for (TicketField field : fields) {
            String value = null;
            
            // If field has a metadata variable, try to resolve it
            if (field.getMetadataVariable() != null && !field.getMetadataVariable().trim().isEmpty() && ticket != null) {
                String variable = field.getMetadataVariable().trim();
                
                // First, try to resolve from User entity for standard fields
                value = resolveFromUserEntity(variable, ticket);
                
                // If not found in User entity, try metadata service (external API)
                if (value == null && metadataService != null) {
                    try {
                        value = metadataService.getMappedValue(variable, ticket);
                    } catch (Exception e) {
                        System.err.println("Failed to resolve metadata variable " + variable + ": " + e.getMessage());
                    }
                }
            }
            
            // Build field data
            Map<String, Object> fieldInfo = new HashMap<>();
            fieldInfo.put("fieldKey", field.getFieldKey());
            fieldInfo.put("fieldLabel", field.getFieldLabel());
            fieldInfo.put("fieldType", field.getFieldType());
            fieldInfo.put("value", value != null ? value : "");
            fieldInfo.put("isEditable", field.getIsEditable());
            fieldInfo.put("icon", field.getIcon());
            fieldInfo.put("description", field.getDescription());
            fieldInfo.put("metadataVariable", field.getMetadataVariable());
            
            fieldData.put(field.getFieldKey(), fieldInfo);
        }
        
        return fieldData;
    }
    
    /**
     * Resolve standard user fields from User entity
     * Supports: {{user.name}}, {{user.email}}, {{user.location}}, {{user.device}}, {{user.phone}}, {{user.uid}}
     */
    private String resolveFromUserEntity(String variable, Ticket ticket) {
        if (ticket == null || ticket.getUser() == null) {
            return null;
        }
        
        // Remove {{ }} if present
        String cleanVar = variable.replace("{{", "").replace("}}", "").trim();
        
        // Parse entity.field format
        String[] parts = cleanVar.split("\\.");
        if (parts.length != 2) {
            return null;
        }
        
        String entity = parts[0].toLowerCase();
        String field = parts[1].toLowerCase();
        
        if (!"user".equals(entity)) {
            return null; // Only handle user entity here
        }
        
        com.prototype.entity.User user = ticket.getUser();
        
        switch (field) {
            case "name":
                return user.getName() != null && !user.getName().trim().isEmpty() ? user.getName() : null;
            case "email":
                return user.getEmail() != null && !user.getEmail().trim().isEmpty() ? user.getEmail() : null;
            case "location":
                return user.getLocation() != null && !user.getLocation().trim().isEmpty() ? user.getLocation() : null;
            case "device":
                return user.getDevice() != null && !user.getDevice().trim().isEmpty() ? user.getDevice() : null;
            case "phone":
            case "phonenumber":
                return user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty() ? user.getPhoneNumber() : null;
            case "uid":
            case "id":
                return user.getUid() != null && !user.getUid().trim().isEmpty() ? user.getUid() : null;
            default:
                return null; // Unknown field, let metadata service handle it
        }
    }
    
    /**
     * Save or update a ticket field
     */
    @Transactional
    public TicketField saveField(TicketField field) {
        return ticketFieldRepository.save(field);
    }
    
    /**
     * Delete a ticket field
     */
    @Transactional
    public void deleteField(Long id) {
        ticketFieldRepository.deleteById(id);
    }
    
    /**
     * Get a field by ID
     */
    @Transactional(readOnly = true)
    public TicketField getFieldById(Long id) {
        return ticketFieldRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket field not found with id: " + id));
    }
}

