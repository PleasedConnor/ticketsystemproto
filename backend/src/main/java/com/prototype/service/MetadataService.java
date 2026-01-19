package com.prototype.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prototype.entity.MetadataConnection;
import com.prototype.entity.MetadataFieldMapping;
import com.prototype.etl.FieldTypeDetector;
import com.prototype.repository.MetadataConnectionRepository;
import com.prototype.repository.MetadataFieldMappingRepository;
import com.prototype.repository.CustomerProfileFieldRepository;
import com.prototype.repository.TicketFieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Service
public class MetadataService {
    
    @Autowired
    private MetadataConnectionRepository connectionRepository;
    
    @Autowired
    private MetadataFieldMappingRepository mappingRepository;
    
    @Autowired(required = false)
    private CustomerProfileFieldRepository customerProfileFieldRepository;
    
    @Autowired(required = false)
    private TicketFieldRepository ticketFieldRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
    
    public List<MetadataConnection> getAllConnections() {
        return connectionRepository.findAll();
    }
    
    public List<MetadataConnection> getActiveConnections() {
        return connectionRepository.findByIsActiveTrue();
    }
    
    @Transactional(readOnly = true)
    public MetadataConnection getConnectionById(Long id) {
        return connectionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Connection not found with id: " + id));
    }
    
    @Transactional
    public MetadataConnection createConnection(MetadataConnection connection) {
        return connectionRepository.save(connection);
    }
    
    @Transactional
    public MetadataConnection updateConnection(Long id, MetadataConnection connection) {
        MetadataConnection existing = getConnectionById(id);
        existing.setName(connection.getName());
        existing.setApiEndpoint(connection.getApiEndpoint());
        existing.setAuthType(connection.getAuthType());
        existing.setAuthCredentials(connection.getAuthCredentials());
        existing.setRequestMethod(connection.getRequestMethod());
        existing.setRequestHeaders(connection.getRequestHeaders());
        existing.setRequestBody(connection.getRequestBody());
        existing.setIsActive(connection.getIsActive());
        return connectionRepository.save(existing);
    }
    
    @Transactional
    public void deleteConnection(Long id) {
        // Delete all mappings first
        List<MetadataFieldMapping> mappings = mappingRepository.findByConnectionId(id);
        mappingRepository.deleteAll(mappings);
        connectionRepository.deleteById(id);
    }
    
    @Transactional(readOnly = true)
    public List<MetadataFieldMapping> getMappingsByConnection(Long connectionId) {
        return mappingRepository.findByConnectionId(connectionId);
    }
    
    public List<MetadataFieldMapping> getActiveMappingsByConnection(Long connectionId) {
        return mappingRepository.findByConnectionIdAndIsActiveTrue(connectionId);
    }
    
    /**
     * Get all mappings from all active connections for AI access management
     * Eagerly loads connection to avoid lazy loading issues
     */
    @Transactional(readOnly = true)
    public List<MetadataFieldMapping> getAllMappingsForAIAccess() {
        try {
            List<MetadataConnection> activeConnections = connectionRepository.findByIsActiveTrue();
            List<MetadataFieldMapping> allMappings = new ArrayList<>();
            
            for (MetadataConnection conn : activeConnections) {
                try {
                    // Use repository method with LEFT JOIN FETCH to eagerly load connection
                    List<MetadataFieldMapping> mappings = mappingRepository.findByConnectionId(conn.getId());
                    for (MetadataFieldMapping mapping : mappings) {
                        if (mapping.getIsActive() != null && mapping.getIsActive()) {
                            // Ensure connection is loaded
                            if (mapping.getConnection() != null) {
                                mapping.getConnection().getName(); // Force load
                            }
                            // Ensure aiAccessible has a default value if null (for existing records)
                            if (mapping.getAiAccessible() == null) {
                                mapping.setAiAccessible(true);
                            }
                            allMappings.add(mapping);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error loading mappings for connection " + conn.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            return allMappings;
        } catch (Exception e) {
            System.err.println("Error in getAllMappingsForAIAccess: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Update AI access for a mapping
     */
    @Transactional
    public MetadataFieldMapping updateAIAccess(Long mappingId, Boolean aiAccessible) {
        MetadataFieldMapping mapping = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new RuntimeException("Mapping not found with id: " + mappingId));
        mapping.setAiAccessible(aiAccessible);
        return mappingRepository.save(mapping);
    }
    
    /**
     * Bulk update AI access for multiple mappings
     */
    @Transactional
    public List<MetadataFieldMapping> bulkUpdateAIAccess(Map<Long, Boolean> accessMap) {
        List<MetadataFieldMapping> updated = new ArrayList<>();
        for (Map.Entry<Long, Boolean> entry : accessMap.entrySet()) {
            MetadataFieldMapping mapping = mappingRepository.findById(entry.getKey())
                .orElseThrow(() -> new RuntimeException("Mapping not found with id: " + entry.getKey()));
            mapping.setAiAccessible(entry.getValue());
            updated.add(mappingRepository.save(mapping));
        }
        return updated;
    }
    
    @Transactional
    public MetadataFieldMapping createMapping(MetadataFieldMapping mapping) {
        return mappingRepository.save(mapping);
    }
    
    @Transactional
    public MetadataFieldMapping updateMapping(Long id, MetadataFieldMapping mapping) {
        MetadataFieldMapping existing = mappingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mapping not found with id: " + id));
        existing.setExternalFieldPath(mapping.getExternalFieldPath());
        existing.setInternalVariable(mapping.getInternalVariable());
        existing.setDataType(mapping.getDataType());
        existing.setDescription(mapping.getDescription());
        existing.setIsActive(mapping.getIsActive());
        if (mapping.getAiAccessible() != null) {
            existing.setAiAccessible(mapping.getAiAccessible());
        }
        return mappingRepository.save(existing);
    }
    
    @Transactional
    public void deleteMapping(Long id) {
        mappingRepository.deleteById(id);
    }
    
    @Transactional
    public void saveMappings(Long connectionId, List<MetadataFieldMapping> mappings) {
        // Delete existing mappings for this connection
        List<MetadataFieldMapping> existing = mappingRepository.findByConnectionId(connectionId);
        mappingRepository.deleteAll(existing);
        
        // Save new mappings
        MetadataConnection connection = getConnectionById(connectionId);
        for (MetadataFieldMapping mapping : mappings) {
            mapping.setConnection(connection);
            mappingRepository.save(mapping);
        }
    }
    
    /**
     * Test API connection and fetch sample data
     */
    public Map<String, Object> testConnection(MetadataConnection connection) {
        // Return fake data for demo connection
        if ("Demo Client API".equals(connection.getName())) {
            return getDemoConnectionData();
        }
        
        try {
            HttpMethod httpMethod = HttpMethod.valueOf(connection.getRequestMethod());
            WebClient.RequestBodySpec requestSpec = webClient
                .method(httpMethod)
                .uri(connection.getApiEndpoint());
            
            // Add headers
            if (connection.getRequestHeaders() != null && !connection.getRequestHeaders().trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> headers = objectMapper.readValue(connection.getRequestHeaders(), Map.class);
                    headers.forEach(requestSpec::header);
                } catch (Exception e) {
                    System.err.println("Failed to parse request headers: " + e.getMessage());
                }
            }
            
            // Add authentication
            addAuthentication(requestSpec, connection);
            
            // Add request body for POST/PUT
            WebClient.ResponseSpec responseSpec;
            if (("POST".equals(connection.getRequestMethod()) || "PUT".equals(connection.getRequestMethod())) 
                && connection.getRequestBody() != null && !connection.getRequestBody().trim().isEmpty()) {
                responseSpec = requestSpec.bodyValue(connection.getRequestBody()).retrieve();
            } else {
                responseSpec = requestSpec.retrieve();
            }
            
            String responseBody = responseSpec
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
            
            // Parse response to extract field paths and types
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            Map<String, String> fieldTypes = new HashMap<>();
            List<String> availableFields = extractFieldPathsWithTypes(jsonResponse, "", fieldTypes);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", responseBody);
            result.put("availableFields", availableFields);
            result.put("fieldTypes", fieldTypes);
            result.put("message", "Connection successful");
            
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Connection failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return result;
        }
    }
    
    /**
     * Add authentication to request
     */
    private void addAuthentication(WebClient.RequestBodySpec requestSpec, MetadataConnection connection) {
        if (connection.getAuthType() == null || connection.getAuthType() == MetadataConnection.AuthType.NONE) {
            return;
        }
        
        try {
            if (connection.getAuthCredentials() == null || connection.getAuthCredentials().trim().isEmpty()) {
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, String> auth = objectMapper.readValue(connection.getAuthCredentials(), Map.class);
            
            switch (connection.getAuthType()) {
                case API_KEY:
                    String apiKeyHeader = auth.getOrDefault("header", "X-API-Key");
                    String apiKey = auth.get("apiKey");
                    if (apiKey != null) {
                        requestSpec.header(apiKeyHeader, apiKey);
                    }
                    break;
                case BEARER_TOKEN:
                    String token = auth.get("token");
                    if (token != null) {
                        requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    }
                    break;
                case BASIC_AUTH:
                    String username = auth.get("username");
                    String password = auth.get("password");
                    if (username != null && password != null) {
                        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                        requestSpec.header(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
                    }
                    break;
                case NONE:
                    // No authentication needed
                    break;
            }
        } catch (Exception e) {
            System.err.println("Failed to add authentication: " + e.getMessage());
        }
    }
    
    /**
     * Extract all field paths from JSON response
     */
    private List<String> extractFieldPaths(JsonNode node, String prefix) {
        List<String> paths = new ArrayList<>();
        
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String currentPath = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                paths.add(currentPath);
                paths.addAll(extractFieldPaths(entry.getValue(), currentPath));
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size() && i < 1; i++) { // Only check first element
                paths.addAll(extractFieldPaths(node.get(i), prefix + "[0]"));
            }
        }
        
        return paths;
    }
    
    /**
     * Extract field paths with their data types
     */
    private List<String> extractFieldPathsWithTypes(JsonNode node, String prefix, Map<String, String> fieldTypes) {
        List<String> paths = new ArrayList<>();
        
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String currentPath = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                JsonNode value = entry.getValue();
                
                // Only add leaf nodes (actual values) to paths
                if (value.isValueNode() || value.isArray()) {
                    String type = FieldTypeDetector.detectType(value);
                    fieldTypes.put(currentPath, type);
                    paths.add(currentPath);
                } else {
                    // Recurse into nested objects
                    paths.addAll(extractFieldPathsWithTypes(value, currentPath, fieldTypes));
                }
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size() && i < 1; i++) { // Only check first element
                paths.addAll(extractFieldPathsWithTypes(node.get(i), prefix + "[0]", fieldTypes));
            }
        } else {
            // Leaf node
            String type = FieldTypeDetector.detectType(node);
            fieldTypes.put(prefix, type);
            paths.add(prefix);
        }
        
        return paths;
    }
    
    @Autowired(required = false)
    private com.prototype.etl.ETLPipeline etlPipeline;
    
    /**
     * Get mapped value for a template variable with user context for data isolation
     * Fetches data dynamically from external APIs based on user/ticket context
     * 
     * @param internalVariable The template variable (e.g., "{{order.total}}")
     * @param ticket The ticket context (contains user info for data isolation)
     * @return The resolved value, or null if not found
     */
    public String getMappedValue(String internalVariable, com.prototype.entity.Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        
        // Find active mapping for this variable that is AI accessible
        List<MetadataFieldMapping> mappings = mappingRepository.findAll().stream()
            .filter(m -> m.getIsActive() 
                    && m.getAiAccessible() != null && m.getAiAccessible() 
                    && internalVariable.equals(m.getInternalVariable()))
            .toList();
        
        if (mappings.isEmpty()) {
            return null; // Variable not accessible by AI
        }
        
        MetadataFieldMapping mapping = mappings.get(0);
        MetadataConnection connection = mapping.getConnection();
        
        if (connection == null || !connection.getIsActive()) {
            return null;
        }
        
        try {
            // Fetch data from external API with user context
            Map<String, Object> externalData = fetchUserData(connection, ticket);
            
            if (externalData == null || externalData.isEmpty()) {
                return null;
            }
            
            // Extract value using ETL pipeline for transformation
            String externalPath = mapping.getExternalFieldPath();
            Object rawValue = extractValueFromPath(externalData, externalPath);
            
            if (rawValue == null) {
                return null;
            }
            
            // Transform value using ETL pipeline
            if (etlPipeline != null) {
                Object transformedValue = etlPipeline.transformValue(rawValue, mapping);
                return transformedValue != null ? transformedValue.toString() : null;
            }
            
            return rawValue.toString();
        } catch (Exception e) {
            System.err.println("Failed to fetch mapped value for " + internalVariable + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Fetch user-specific data from external API with context isolation
     * Ensures user can only access their own data
     */
    private Map<String, Object> fetchUserData(MetadataConnection connection, com.prototype.entity.Ticket ticket) {
        try {
            // Build dynamic endpoint with user context
            String dynamicEndpoint = buildDynamicEndpoint(connection.getApiEndpoint(), ticket);
            
            HttpMethod httpMethod = HttpMethod.valueOf(connection.getRequestMethod());
            WebClient.RequestBodySpec requestSpec = webClient
                .method(httpMethod)
                .uri(dynamicEndpoint);
            
            // Add headers
            if (connection.getRequestHeaders() != null && !connection.getRequestHeaders().trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> headers = objectMapper.readValue(connection.getRequestHeaders(), Map.class);
                    headers.forEach((key, value) -> {
                        // Replace template variables in headers
                        String dynamicValue = replaceVariablesInString(value, ticket);
                        requestSpec.header(key, dynamicValue);
                    });
                } catch (Exception e) {
                    System.err.println("Failed to parse request headers: " + e.getMessage());
                }
            }
            
            // Add authentication
            addAuthentication(requestSpec, connection);
            
            // Build dynamic request body with user context
            String dynamicBody = connection.getRequestBody();
            if (dynamicBody != null && !dynamicBody.trim().isEmpty()) {
                dynamicBody = replaceVariablesInString(dynamicBody, ticket);
            }
            
            // Add request body for POST/PUT
            WebClient.ResponseSpec responseSpec;
            if (("POST".equals(connection.getRequestMethod()) || "PUT".equals(connection.getRequestMethod())) 
                && dynamicBody != null && !dynamicBody.trim().isEmpty()) {
                try {
                    JsonNode bodyNode = objectMapper.readTree(dynamicBody);
                    responseSpec = requestSpec.bodyValue(bodyNode).retrieve();
                } catch (Exception e) {
                    responseSpec = requestSpec.bodyValue(dynamicBody).retrieve();
                }
            } else {
                responseSpec = requestSpec.retrieve();
            }
            
            String responseBody = responseSpec
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return null;
            }
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.convertValue(jsonResponse, Map.class);
            
            return data;
        } catch (Exception e) {
            System.err.println("Failed to fetch user data from " + connection.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Build dynamic API endpoint with user/ticket context
     * Supports variables like {{user.id}}, {{ticket.id}}, etc.
     */
    private String buildDynamicEndpoint(String endpoint, com.prototype.entity.Ticket ticket) {
        if (endpoint == null || ticket == null) {
            return endpoint;
        }
        
        String result = endpoint;
        
        // Replace user variables
        if (ticket.getUser() != null) {
            com.prototype.entity.User user = ticket.getUser();
            result = result.replace("{{user.id}}", user.getUid() != null ? user.getUid() : "");
            result = result.replace("{{user.email}}", user.getEmail() != null ? user.getEmail() : "");
            result = result.replace("{{user.name}}", user.getName() != null ? user.getName() : "");
        }
        
        // Replace ticket variables
        result = result.replace("{{ticket.id}}", ticket.getId() != null ? ticket.getId().toString() : "");
        result = result.replace("{{ticket.status}}", ticket.getStatus() != null ? ticket.getStatus().toString() : "");
        
        return result;
    }
    
    /**
     * Replace template variables in string (for headers, body, etc.)
     */
    private String replaceVariablesInString(String text, com.prototype.entity.Ticket ticket) {
        if (text == null || ticket == null) {
            return text;
        }
        
        String result = text;
        
        // Replace user variables
        if (ticket.getUser() != null) {
            com.prototype.entity.User user = ticket.getUser();
            result = result.replace("{{user.id}}", user.getUid() != null ? user.getUid() : "");
            result = result.replace("{{user.email}}", user.getEmail() != null ? user.getEmail() : "");
            result = result.replace("{{user.name}}", user.getName() != null ? user.getName() : "");
        }
        
        // Replace ticket variables
        result = result.replace("{{ticket.id}}", ticket.getId() != null ? ticket.getId().toString() : "");
        result = result.replace("{{ticket.status}}", ticket.getStatus() != null ? ticket.getStatus().toString() : "");
        
        return result;
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
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Test a mapping value for a specific ticket/user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> testMappingValue(Long mappingId, Long ticketId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            MetadataFieldMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new RuntimeException("Mapping not found with id: " + mappingId));
            
            result.put("mappingId", mappingId);
            result.put("internalVariable", mapping.getInternalVariable());
            result.put("externalFieldPath", mapping.getExternalFieldPath());
            result.put("aiAccessible", mapping.getAiAccessible());
            
            // If ticketId provided, note it (for future implementation with real ticket context)
            if (ticketId != null) {
                result.put("ticketId", ticketId);
                result.put("note", "Using demo data. In production, this would fetch data for the specific user/ticket.");
            }
            
            // Get demo data to show what the value would be
            MetadataConnection connection = mapping.getConnection();
            if (connection != null && "Demo Client API".equals(connection.getName())) {
                // Parse the demo JSON response
                Map<String, Object> demoData = new HashMap<>();
                demoData.put("user", Map.of(
                    "name", "John Doe",
                    "email", "john.doe@example.com",
                    "location", "New York",
                    "device", "iPhone 14"
                ));
                demoData.put("order", Map.of(
                    "id", "ORD-12345",
                    "status", "pending",
                    "total", 299.99
                ));
                
                Object value = extractValueFromPath(demoData, mapping.getExternalFieldPath());
                if (value != null) {
                    result.put("testValue", value.toString());
                    result.put("dataType", mapping.getDataType());
                    result.put("success", true);
                } else {
                    result.put("testValue", null);
                    result.put("success", false);
                    result.put("error", "Could not extract value from demo data for path: " + mapping.getExternalFieldPath());
                }
            } else {
                result.put("testValue", null);
                result.put("success", false);
                result.put("error", "Only demo connections can be tested. Real API connections require a valid ticket context.");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get fake demo data for testing
     */
    private Map<String, Object> getDemoConnectionData() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Demo connection - using sample data");
        
        // Create fake JSON response
        Map<String, Object> demoData = new HashMap<>();
        demoData.put("user", Map.of(
            "name", "John Doe",
            "email", "john.doe@example.com",
            "location", "New York",
            "device", "iPhone 14"
        ));
        demoData.put("order", Map.of(
            "id", "ORD-12345",
            "status", "pending",
            "total", 299.99
        ));
        
        try {
            String jsonResponse = objectMapper.writeValueAsString(demoData);
            result.put("response", jsonResponse);
            
            // Extract field paths with types
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            Map<String, String> fieldTypes = new HashMap<>();
            List<String> availableFields = extractFieldPathsWithTypes(jsonNode, "", fieldTypes);
            result.put("availableFields", availableFields);
            result.put("fieldTypes", fieldTypes);
        } catch (Exception e) {
            System.err.println("Failed to create demo data: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Failed to generate demo data");
        }
        
        return result;
    }
    
    /**
     * Get available metadata variables (not already in use by chat/ticket fields)
     * @param excludeVariable Optional variable to exclude from the "in use" check (for editing existing fields)
     * @return List of available metadata variable strings
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableMetadataVariables(String excludeVariable) {
        // Start with standard user fields (always available)
        Set<String> allVariables = new HashSet<>(List.of(
            "{{user.name}}",
            "{{user.email}}",
            "{{user.location}}",
            "{{user.device}}",
            "{{user.phone}}",
            "{{user.uid}}"
        ));
        
        // Get all active metadata mappings
        List<MetadataFieldMapping> allMappings = mappingRepository.findAll().stream()
            .filter(m -> m.getIsActive() != null && m.getIsActive())
            .toList();
        
        for (MetadataFieldMapping mapping : allMappings) {
            if (mapping.getInternalVariable() != null && !mapping.getInternalVariable().trim().isEmpty()) {
                allVariables.add(mapping.getInternalVariable().trim());
            }
        }
        
        // Get variables already in use by customer profile fields
        Set<String> usedVariables = new HashSet<>();
        if (customerProfileFieldRepository != null) {
            customerProfileFieldRepository.findAll().stream()
                .filter(f -> f.getMetadataVariable() != null && !f.getMetadataVariable().trim().isEmpty())
                .forEach(f -> {
                    String var = f.getMetadataVariable().trim();
                    // Exclude the variable being edited
                    if (excludeVariable == null || !var.equals(excludeVariable)) {
                        usedVariables.add(var);
                    }
                });
        }
        
        // Get variables already in use by ticket fields
        if (ticketFieldRepository != null) {
            ticketFieldRepository.findAll().stream()
                .filter(f -> f.getMetadataVariable() != null && !f.getMetadataVariable().trim().isEmpty())
                .forEach(f -> {
                    String var = f.getMetadataVariable().trim();
                    // Exclude the variable being edited
                    if (excludeVariable == null || !var.equals(excludeVariable)) {
                        usedVariables.add(var);
                    }
                });
        }
        
        // Return available variables (all variables minus used ones)
        List<String> available = new ArrayList<>(allVariables);
        available.removeAll(usedVariables);
        available.sort(String::compareTo);
        
        return available;
    }
}

