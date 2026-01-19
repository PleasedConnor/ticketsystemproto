package com.prototype.service;

import com.prototype.entity.CustomerProfileField;
import com.prototype.entity.Ticket;
import com.prototype.repository.CustomerProfileFieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing customer profile fields and resolving their values from metadata
 */
@Service
public class CustomerProfileService {
    
    @Autowired
    private CustomerProfileFieldRepository profileFieldRepository;
    
    @Autowired(required = false)
    private MetadataService metadataService;
    
    /**
     * Get all visible customer profile fields ordered by display order
     */
    @Transactional(readOnly = true)
    public List<CustomerProfileField> getAllVisibleFields() {
        return profileFieldRepository.findAllVisibleOrdered();
    }
    
    /**
     * Get all customer profile fields (including hidden)
     */
    @Transactional(readOnly = true)
    public List<CustomerProfileField> getAllFields() {
        return profileFieldRepository.findAll();
    }
    
    /**
     * Get customer profile data with resolved values for a specific ticket/user
     * Resolves metadata variables to actual values
     * Priority: 1. User entity data (for standard fields), 2. Metadata mappings (external API)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerProfileData(Ticket ticket) {
        List<CustomerProfileField> fields = profileFieldRepository.findAllVisibleOrdered();
        Map<String, Object> profileData = new HashMap<>();
        
        for (CustomerProfileField field : fields) {
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
            Map<String, Object> fieldData = new HashMap<>();
            fieldData.put("fieldKey", field.getFieldKey());
            fieldData.put("fieldLabel", field.getFieldLabel());
            fieldData.put("fieldType", field.getFieldType());
            fieldData.put("value", value != null ? value : "");
            fieldData.put("isEditable", field.getIsEditable());
            fieldData.put("icon", field.getIcon());
            fieldData.put("description", field.getDescription());
            fieldData.put("metadataVariable", field.getMetadataVariable());
            
            profileData.put(field.getFieldKey(), fieldData);
        }
        
        return profileData;
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
     * Save or update a customer profile field
     */
    @Transactional
    public CustomerProfileField saveField(CustomerProfileField field) {
        return profileFieldRepository.save(field);
    }
    
    /**
     * Delete a customer profile field
     */
    @Transactional
    public void deleteField(Long id) {
        profileFieldRepository.deleteById(id);
    }
    
    /**
     * Get a field by ID
     */
    @Transactional(readOnly = true)
    public CustomerProfileField getFieldById(Long id) {
        return profileFieldRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer profile field not found with id: " + id));
    }
}

