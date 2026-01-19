package com.prototype.rag.storage;

import com.prototype.rag.chunking.DocumentChunk;

import java.util.List;

/**
 * Interface for storing and retrieving document chunks with embeddings
 */
public interface VectorStore {
    
    /**
     * Add a document chunk to the store
     * @param chunk The document chunk with embedding
     */
    void addChunk(DocumentChunk chunk);
    
    /**
     * Add multiple chunks to the store
     * @param chunks List of document chunks
     */
    void addChunks(List<DocumentChunk> chunks);
    
    /**
     * Remove all chunks for a specific article
     * @param articleId The article ID
     */
    void removeChunksByArticleId(Long articleId);
    
    /**
     * Search for similar chunks using cosine similarity
     * @param queryEmbedding The embedding of the query
     * @param topK Number of results to return
     * @return List of similar document chunks, sorted by similarity (highest first)
     */
    List<DocumentChunk> searchSimilar(List<Float> queryEmbedding, int topK);
    
    /**
     * Get the total number of chunks in the store
     * @return Number of chunks
     */
    int getChunkCount();
    
    /**
     * Clear all chunks from the store
     */
    void clear();
}

