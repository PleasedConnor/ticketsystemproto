package com.prototype.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding service using Ollama's embedding API
 * Uses nomic-embed-text model (good quality, fast, 768 dimensions)
 */
@Service
public class OllamaEmbeddingService implements EmbeddingService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String EMBEDDING_MODEL = "nomic-embed-text";
    private static final int EMBEDDING_DIMENSION = 768; // nomic-embed-text produces 768-dim vectors
    
    public OllamaEmbeddingService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://localhost:11434")
            .build();
        this.objectMapper = new ObjectMapper();
        System.out.println("OllamaEmbeddingService initialized with model: " + EMBEDDING_MODEL);
    }
    
    @Override
    public List<Float> generateEmbedding(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", EMBEDDING_MODEL);
        requestBody.put("prompt", text);
        
        try {
            String response = webClient.post()
                .uri("/api/embeddings")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode embeddingArray = jsonResponse.get("embedding");
            
            if (embeddingArray == null || !embeddingArray.isArray()) {
                throw new RuntimeException("Invalid embedding response from Ollama");
            }
            
            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingArray) {
                embedding.add((float) value.asDouble());
            }
            
            return embedding;
        } catch (Exception e) {
            System.err.println("Failed to generate embedding: " + e.getMessage());
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) throws Exception {
        List<List<Float>> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }
    
    @Override
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }
}

