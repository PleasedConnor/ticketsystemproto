package com.prototype.rag.embedding;

import java.util.List;

/**
 * Service for generating embeddings from text
 */
public interface EmbeddingService {
    
    /**
     * Generate embedding for a single text
     * @param text The text to embed
     * @return List of floats representing the embedding vector
     */
    List<Float> generateEmbedding(String text) throws Exception;
    
    /**
     * Generate embeddings for multiple texts (batch)
     * @param texts List of texts to embed
     * @return List of embeddings (one per text)
     */
    List<List<Float>> generateEmbeddings(List<String> texts) throws Exception;
    
    /**
     * Get the dimension of embeddings produced by this service
     * @return Embedding dimension
     */
    int getEmbeddingDimension();
}

