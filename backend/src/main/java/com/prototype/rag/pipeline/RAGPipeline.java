package com.prototype.rag.pipeline;

import com.prototype.rag.chunking.DocumentChunk;

import java.util.List;

/**
 * RAG Pipeline orchestrator
 * Coordinates the entire RAG flow: retrieval → context building → generation
 */
public interface RAGPipeline {
    
    /**
     * Retrieve relevant context for a query
     * @param query The user query
     * @param maxChunks Maximum number of chunks to retrieve
     * @return Formatted context string ready for AI prompt
     */
    String retrieveContext(String query, int maxChunks) throws Exception;
    
    /**
     * Retrieve relevant context with category filtering
     * @param query The user query
     * @param maxChunks Maximum number of chunks to retrieve
     * @param category Optional category filter
     * @return Formatted context string ready for AI prompt
     */
    String retrieveContext(String query, int maxChunks, String category) throws Exception;
    
    /**
     * Get the retrieved chunks (for debugging/inspection)
     * @param query The user query
     * @param maxChunks Maximum number of chunks
     * @return List of retrieved chunks
     */
    List<DocumentChunk> getRetrievedChunks(String query, int maxChunks) throws Exception;
}

