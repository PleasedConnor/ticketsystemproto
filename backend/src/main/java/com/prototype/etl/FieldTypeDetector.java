package com.prototype.etl;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Detects the data type of a field value from JSON responses
 */
public class FieldTypeDetector {
    
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^-?\\d+\\.\\d+$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false|yes|no|1|0)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.+");
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };
    
    /**
     * Detect the data type of a JSON node value
     */
    public static String detectType(JsonNode node) {
        if (node == null || node.isNull()) {
            return "NULL";
        }
        
        if (node.isBoolean()) {
            return "BOOLEAN";
        }
        
        if (node.isInt() || node.isLong()) {
            return "INTEGER";
        }
        
        if (node.isDouble() || node.isFloat()) {
            return "DECIMAL";
        }
        
        if (node.isTextual()) {
            return detectStringType(node.asText());
        }
        
        if (node.isArray()) {
            return "ARRAY";
        }
        
        if (node.isObject()) {
            return "OBJECT";
        }
        
        return "STRING"; // Default fallback
    }
    
    /**
     * Detect the type of a string value
     */
    private static String detectStringType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "STRING";
        }
        
        // Check for integer
        if (INTEGER_PATTERN.matcher(value).matches()) {
            return "INTEGER";
        }
        
        // Check for decimal
        if (DECIMAL_PATTERN.matcher(value).matches()) {
            return "DECIMAL";
        }
        
        // Check for boolean
        if (BOOLEAN_PATTERN.matcher(value).matches()) {
            return "BOOLEAN";
        }
        
        // Check for date/time
        if (isDate(value)) {
            return "DATETIME";
        }
        
        // Check for email
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return "EMAIL";
        }
        
        // Check for URL
        if (URL_PATTERN.matcher(value).matches()) {
            return "URL";
        }
        
        return "STRING";
    }
    
    /**
     * Check if a string is a date/time
     */
    private static boolean isDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDateTime.parse(value, formatter);
                return true;
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        return false;
    }
}

