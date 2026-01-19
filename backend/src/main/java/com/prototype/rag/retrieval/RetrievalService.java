package com.prototype.rag.retrieval;

import com.prototype.rag.chunking.DocumentChunk;

import java.util.List;

/**
 * Service for retrieving relevant document chunks based on semantic similarity
 */
public interface RetrievalService {
    
    /**
     * Retrieve relevant chunks for a query
     * @param query The search query
     * @param topK Number of results to return
     * @return List of relevant document chunks
     */
    List<DocumentChunk> retrieve(String query, int topK) throws Exception;
    
    /**
     * Retrieve relevant chunks with metadata filtering
     * @param query The search query
     * @param topK Number of results to return
     * @param category Optional category filter (null for all categories)
     * @return List of relevant document chunks
     */
    List<DocumentChunk> retrieve(String query, int topK, String category) throws Exception;
}

