package com.prototype.etl;

import com.prototype.entity.MetadataFieldMapping;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ETL Pipeline for transforming external data to internal format
 * Extract: Get data from external sources (handled by MetadataService)
 * Transform: Convert data types, formats, validate, clean
 * Load: Map to internal template variables (handled by MetadataService)
 */
@Service
public class ETLPipeline {
    
    /**
     * Transform a value based on mapping rules and target data type
     */
    public Object transformValue(Object rawValue, MetadataFieldMapping mapping) {
        if (rawValue == null) {
            return null;
        }
        
        String targetType = mapping.getDataType();
        if (targetType == null || targetType.isEmpty()) {
            // No transformation specified, return as string
            return rawValue.toString();
        }
        
        try {
            switch (targetType.toUpperCase()) {
                case "STRING":
                    return transformToString(rawValue);
                case "INTEGER":
                    return transformToInteger(rawValue);
                case "DECIMAL":
                case "FLOAT":
                case "DOUBLE":
                    return transformToDecimal(rawValue);
                case "BOOLEAN":
                    return transformToBoolean(rawValue);
                case "DATETIME":
                case "DATE":
                    return transformToDateTime(rawValue);
                case "EMAIL":
                    return transformToEmail(rawValue);
                case "URL":
                    return transformToUrl(rawValue);
                default:
                    return rawValue.toString();
            }
        } catch (Exception e) {
            System.err.println("Failed to transform value " + rawValue + " to " + targetType + ": " + e.getMessage());
            return rawValue.toString(); // Fallback to string
        }
    }
    
    /**
     * Transform entire data object using all active mappings
     */
    public Map<String, Object> transformData(Map<String, Object> rawData, List<MetadataFieldMapping> mappings) {
        Map<String, Object> transformed = new HashMap<>();
        
        for (MetadataFieldMapping mapping : mappings) {
            if (!mapping.getIsActive()) {
                continue;
            }
            
            String externalPath = mapping.getExternalFieldPath();
            Object rawValue = extractValueFromPath(rawData, externalPath);
            
            if (rawValue != null) {
                Object transformedValue = transformValue(rawValue, mapping);
                String internalVar = mapping.getInternalVariable();
                // Store with internal variable name (without {{}})
                String varKey = internalVar.replace("{{", "").replace("}}", "");
                transformed.put(varKey, transformedValue);
            }
        }
        
        return transformed;
    }
    
    /**
     * Extract value from nested map using dot notation path
     */
    @SuppressWarnings("unchecked")
    private Object extractValueFromPath(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else if (current instanceof List && part.matches("\\[\\d+\\]")) {
                int index = Integer.parseInt(part.substring(1, part.length() - 1));
                List<?> list = (List<?>) current;
                if (index >= 0 && index < list.size()) {
                    current = list.get(index);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    // Transformation methods
    
    private String transformToString(Object value) {
        return value.toString().trim();
    }
    
    private Integer transformToInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String str = value.toString().trim();
        // Remove commas, currency symbols, etc.
        str = str.replaceAll("[^\\d-]", "");
        return Integer.parseInt(str);
    }
    
    private Double transformToDecimal(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String str = value.toString().trim();
        // Remove currency symbols, commas, etc.
        str = str.replaceAll("[^\\d.-]", "");
        return Double.parseDouble(str);
    }
    
    private Boolean transformToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = value.toString().trim().toLowerCase();
        return str.equals("true") || str.equals("yes") || str.equals("1") || str.equals("on");
    }
    
    private String transformToDateTime(Object value) {
        // For now, return as ISO string. Could add format conversion later
        return value.toString();
    }
    
    private String transformToEmail(Object value) {
        String email = value.toString().trim().toLowerCase();
        // Basic validation
        if (email.contains("@") && email.contains(".")) {
            return email;
        }
        throw new IllegalArgumentException("Invalid email format: " + email);
    }
    
    private String transformToUrl(Object value) {
        String url = value.toString().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return url;
    }
}

