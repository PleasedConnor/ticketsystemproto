package com.prototype.rag.storage;

import com.prototype.rag.chunking.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of VectorStore
 * Uses cosine similarity for search
 */
@Component
public class InMemoryVectorStore implements VectorStore {
    
    private final Map<String, DocumentChunk> chunks = new ConcurrentHashMap<>();
    
    @Override
    public void addChunk(DocumentChunk chunk) {
        if (chunk == null || chunk.getId() == null) {
            throw new IllegalArgumentException("Chunk and chunk ID cannot be null");
        }
        chunks.put(chunk.getId(), chunk);
    }
    
    @Override
    public void addChunks(List<DocumentChunk> chunks) {
        if (chunks == null) {
            return;
        }
        for (DocumentChunk chunk : chunks) {
            addChunk(chunk);
        }
    }
    
    @Override
    public void removeChunksByArticleId(Long articleId) {
        if (articleId == null) {
            return;
        }
        chunks.entrySet().removeIf(entry -> 
            entry.getValue().getArticleId() != null && 
            entry.getValue().getArticleId().equals(articleId)
        );
    }
    
    @Override
    public List<DocumentChunk> searchSimilar(List<Float> queryEmbedding, int topK) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return List.of();
        }
        
        if (chunks.isEmpty()) {
            return List.of();
        }
        
        // Calculate cosine similarity for each chunk
        List<Map.Entry<DocumentChunk, Double>> scoredChunks = new ArrayList<>();
        
        for (DocumentChunk chunk : chunks.values()) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().isEmpty()) {
                continue;
            }
            
            double similarity = cosineSimilarity(queryEmbedding, chunk.getEmbedding());
            // Increased threshold to 0.65 for much better relevance - only return highly relevant articles
            // This prevents irrelevant articles from being returned when Knowledge Base is small
            if (similarity > 0.65) {
                scoredChunks.add(new AbstractMap.SimpleEntry<>(chunk, similarity));
                System.out.println("  VectorStore: Chunk " + chunk.getId() + " (Article: " + chunk.getTitle() + ") similarity: " + String.format("%.4f", similarity) + " ✓ (above 0.65 threshold)");
            } else {
                System.out.println("  VectorStore: Chunk " + chunk.getId() + " (Article: " + chunk.getTitle() + ") similarity: " + String.format("%.4f", similarity) + " ✗ (below 0.65 threshold - not relevant enough)");
            }
        }
        
        // Sort by similarity (descending) and return top K
        scoredChunks.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        return scoredChunks.stream()
            .limit(topK)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate cosine similarity between two vectors
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Cosine similarity (0 to 1, where 1 is identical)
     */
    private double cosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            float v1 = vector1.get(i);
            float v2 = vector2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    @Override
    public int getChunkCount() {
        return chunks.size();
    }
    
    @Override
    public void clear() {
        chunks.clear();
    }
}

