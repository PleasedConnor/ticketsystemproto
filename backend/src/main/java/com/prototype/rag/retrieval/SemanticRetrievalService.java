package com.prototype.rag.retrieval;

import com.prototype.rag.chunking.DocumentChunk;
import com.prototype.rag.embedding.EmbeddingService;
import com.prototype.rag.storage.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Semantic retrieval service using embeddings and vector similarity
 */
@Service
public class SemanticRetrievalService implements RetrievalService {
    
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    
    @Autowired
    public SemanticRetrievalService(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        System.out.println("SemanticRetrievalService initialized");
    }
    
    @Override
    public List<DocumentChunk> retrieve(String query, int topK) throws Exception {
        return retrieve(query, topK, null);
    }
    
    @Override
    public List<DocumentChunk> retrieve(String query, int topK, String category) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        
        System.out.println("SemanticRetrievalService: Retrieving for query: '" + query + "' (topK: " + topK + ")");
        
        // Generate embedding for the query
        List<Float> queryEmbedding = embeddingService.generateEmbedding(query);
        System.out.println("SemanticRetrievalService: Generated query embedding (dimension: " + queryEmbedding.size() + ")");
        
        // Search for similar chunks (get more results if filtering by category)
        int searchTopK = category != null ? topK * 2 : topK;
        List<DocumentChunk> similarChunks = vectorStore.searchSimilar(queryEmbedding, searchTopK);
        
        System.out.println("SemanticRetrievalService: Found " + similarChunks.size() + " similar chunks");
        
        // Log details about retrieved chunks with similarity scores (if available from vector store)
        System.out.println("SemanticRetrievalService: Retrieved " + similarChunks.size() + " chunks above similarity threshold");
        for (int i = 0; i < Math.min(5, similarChunks.size()); i++) {
            DocumentChunk chunk = similarChunks.get(i);
            System.out.println("SemanticRetrievalService: Chunk " + (i+1) + " - Article ID: " + chunk.getArticleId() + 
                ", Title: " + chunk.getTitle() + 
                ", Content length: " + (chunk.getContent() != null ? chunk.getContent().length() : 0) +
                ", Preview: " + (chunk.getContent() != null && chunk.getContent().length() > 50 ? 
                chunk.getContent().substring(0, Math.min(50, chunk.getContent().length())) + "..." : chunk.getContent()));
        }
        
        // Filter by category if specified
        if (category != null && !category.trim().isEmpty()) {
            similarChunks = similarChunks.stream()
                .filter(chunk -> category.equalsIgnoreCase(chunk.getCategory()))
                .limit(topK)
                .collect(Collectors.toList());
            System.out.println("SemanticRetrievalService: Filtered to " + similarChunks.size() + " chunks in category: " + category);
        } else {
            // Limit to topK if not filtering
            similarChunks = similarChunks.stream()
                .limit(topK)
                .collect(Collectors.toList());
        }
        
        return similarChunks;
    }
}

