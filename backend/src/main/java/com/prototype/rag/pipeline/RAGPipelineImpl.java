package com.prototype.rag.pipeline;

import com.prototype.rag.chunking.DocumentChunk;
import com.prototype.rag.retrieval.RetrievalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Implementation of RAG Pipeline
 * Orchestrates retrieval and context building
 */
@Service
public class RAGPipelineImpl implements RAGPipeline {
    
    private final RetrievalService retrievalService;
    private static final int DEFAULT_MAX_CHUNKS = 3;
    
    @Autowired
    public RAGPipelineImpl(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
        System.out.println("RAGPipelineImpl initialized");
    }
    
    @Override
    public String retrieveContext(String query, int maxChunks) throws Exception {
        return retrieveContext(query, maxChunks, null);
    }
    
    @Override
    public String retrieveContext(String query, int maxChunks, String category) throws Exception {
        List<DocumentChunk> chunks = getRetrievedChunks(query, maxChunks, category);
        
        if (chunks.isEmpty()) {
            System.out.println("RAGPipeline: No relevant chunks found for query: '" + query + "' - returning null to avoid irrelevant context");
            return null;
        }
        
        System.out.println("RAGPipeline: Found " + chunks.size() + " relevant chunks for query: '" + query + "'");
        
        // Build formatted context
        StringBuilder context = new StringBuilder();
        
        // Group chunks by article to avoid duplicates
        Set<Long> seenArticleIds = new java.util.HashSet<>();
        
        // Sort chunks by relevance (assuming first chunks are most relevant from vector store)
        // Only include top chunks per article to avoid repetition
        for (DocumentChunk chunk : chunks) {
            // Only include one chunk per article to avoid repetition
            if (seenArticleIds.contains(chunk.getArticleId())) {
                System.out.println("RAGPipeline: Skipping duplicate article ID: " + chunk.getArticleId() + " (" + chunk.getTitle() + ")");
                continue;
            }
            seenArticleIds.add(chunk.getArticleId());
            
            System.out.println("RAGPipeline: Processing chunk from article ID: " + chunk.getArticleId());
            System.out.println("RAGPipeline: Chunk title: " + chunk.getTitle());
            System.out.println("RAGPipeline: Chunk content length: " + (chunk.getContent() != null ? chunk.getContent().length() : 0));
            System.out.println("RAGPipeline: Chunk content preview: " + (chunk.getContent() != null && chunk.getContent().length() > 100 ? chunk.getContent().substring(0, 100) + "..." : chunk.getContent()));
            
            context.append("Article: ").append(chunk.getTitle()).append("\n");
            if (chunk.getCategory() != null && !chunk.getCategory().trim().isEmpty()) {
                context.append("Category: ").append(chunk.getCategory()).append("\n");
            }
            context.append("Content: ").append(chunk.getContent()).append("\n\n");
        }
        
        String contextString = context.toString();
        System.out.println("RAGPipeline: Generated context (length: " + contextString.length() + " chars, articles: " + seenArticleIds.size() + ")");
        System.out.println("RAGPipeline: Full context: " + contextString);
        
        return contextString;
    }
    
    @Override
    public List<DocumentChunk> getRetrievedChunks(String query, int maxChunks) throws Exception {
        return getRetrievedChunks(query, maxChunks, null);
    }
    
    private List<DocumentChunk> getRetrievedChunks(String query, int maxChunks, String category) throws Exception {
        int actualMaxChunks = maxChunks > 0 ? maxChunks : DEFAULT_MAX_CHUNKS;
        return retrievalService.retrieve(query, actualMaxChunks, category);
    }
}

