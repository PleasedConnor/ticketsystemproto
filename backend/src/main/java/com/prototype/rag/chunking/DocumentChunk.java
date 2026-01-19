package com.prototype.rag.chunking;

import java.util.List;

/**
 * Represents a chunk of a document with its embedding
 */
public class DocumentChunk {
    private String id;
    private Long articleId;
    private String title;
    private String content;
    private int chunkIndex;
    private int totalChunks;
    private List<Float> embedding;
    private String category;
    
    public DocumentChunk() {}
    
    public DocumentChunk(String id, Long articleId, String title, String content, 
                        int chunkIndex, int totalChunks, List<Float> embedding, String category) {
        this.id = id;
        this.articleId = articleId;
        this.title = title;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.embedding = embedding;
        this.category = category;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    
    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    
    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}

