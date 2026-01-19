package com.prototype.rag.chunking;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for chunking documents into smaller pieces for better retrieval
 */
public class DocumentChunker {
    
    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 100;
    
    private final int chunkSize;
    private final int chunkOverlap;
    
    public DocumentChunker() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }
    
    public DocumentChunker(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }
    
    /**
     * Chunk a document into smaller pieces
     * @param text The text to chunk
     * @return List of text chunks
     */
    public List<String> chunkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }
        
        // Strip HTML and normalize whitespace
        String cleanText = text.replaceAll("<[^>]*>", " ")
                              .replaceAll("\\s+", " ")
                              .trim();
        
        if (cleanText.length() <= chunkSize) {
            return List.of(cleanText);
        }
        
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < cleanText.length()) {
            int end = Math.min(start + chunkSize, cleanText.length());
            
            // Try to break at word boundary
            if (end < cleanText.length()) {
                // Look for the last space before the chunk size limit
                int lastSpace = cleanText.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            
            String chunk = cleanText.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            // Move start position with overlap
            start = end - chunkOverlap;
            if (start <= 0) {
                start = end;
            }
        }
        
        return chunks;
    }
    
    /**
     * Chunk an article into DocumentChunk objects
     * @param articleId The article ID
     * @param title The article title
     * @param content The article content
     * @param category The article category (optional)
     * @return List of DocumentChunk objects
     */
    public List<DocumentChunk> chunkArticle(Long articleId, String title, String content, String category) {
        List<String> textChunks = chunkText(content);
        List<DocumentChunk> chunks = new ArrayList<>();
        
        for (int i = 0; i < textChunks.size(); i++) {
            String chunkId = articleId + "_chunk_" + i;
            DocumentChunk chunk = new DocumentChunk();
            chunk.setId(chunkId);
            chunk.setArticleId(articleId);
            chunk.setTitle(title);
            chunk.setContent(textChunks.get(i));
            chunk.setChunkIndex(i);
            chunk.setTotalChunks(textChunks.size());
            chunk.setCategory(category);
            chunks.add(chunk);
        }
        
        return chunks;
    }
}

