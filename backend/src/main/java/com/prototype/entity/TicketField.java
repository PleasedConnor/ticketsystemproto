package com.prototype.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Defines a customizable field displayed on the Tickets page
 * These fields can be mapped to metadata variables for dynamic data display
 */
@Entity
@Table(name = "ticket_fields")
public class TicketField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "field_label", nullable = false, length = 100)
    private String fieldLabel; // Display name (e.g., "Customer Name", "Order Total")
    
    @Column(name = "field_key", nullable = false, length = 100, unique = true)
    private String fieldKey; // Internal key (e.g., "customer_name", "order_total")
    
    @Column(name = "metadata_variable", length = 100)
    private String metadataVariable; // Mapped variable (e.g., "{{user.name}}", "{{order.total}}")
    
    @Column(name = "field_type", nullable = false, length = 50)
    private String fieldType; // TEXT, NUMBER, EMAIL, PHONE, DATE, BOOLEAN, etc.
    
    @Column(name = "display_location", nullable = false, length = 50)
    private String displayLocation; // CUSTOMER_INFO, TICKET_DETAIL, LIST_VIEW, etc.
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0; // Order in which fields appear
    
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true; // Whether field is shown
    
    @Column(name = "is_editable", nullable = false)
    private Boolean isEditable = false; // Whether field can be edited
    
    @Column(name = "icon", length = 50)
    private String icon; // Optional icon (e.g., "user", "email", "phone")
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Optional description/help text
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public TicketField() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    
    public String getMetadataVariable() { return metadataVariable; }
    public void setMetadataVariable(String metadataVariable) { this.metadataVariable = metadataVariable; }
    
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    
    public String getDisplayLocation() { return displayLocation; }
    public void setDisplayLocation(String displayLocation) { this.displayLocation = displayLocation; }
    
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    
    public Boolean getIsVisible() { return isVisible; }
    public void setIsVisible(Boolean isVisible) { this.isVisible = isVisible; }
    
    public Boolean getIsEditable() { return isEditable; }
    public void setIsEditable(Boolean isEditable) { this.isEditable = isEditable; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

