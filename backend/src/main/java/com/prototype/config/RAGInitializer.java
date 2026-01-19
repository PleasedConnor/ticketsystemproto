package com.prototype.config;

import com.prototype.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Initialize RAG system by indexing existing articles
 * Runs after DataInitializer to ensure articles exist
 */
@Component
@Order(2) // Run after DataInitializer
public class RAGInitializer implements CommandLineRunner {
    
    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;
    
    @Override
    public void run(String... args) throws Exception {
        if (knowledgeBaseService != null) {
            System.out.println("Initializing RAG system...");
            try {
                knowledgeBaseService.reindexAllArticles();
                System.out.println("RAG system initialized successfully");
            } catch (Exception e) {
                System.err.println("Failed to initialize RAG system: " + e.getMessage());
                System.err.println("RAG will fall back to keyword-based search");
            }
        } else {
            System.out.println("KnowledgeBaseService not available, skipping RAG initialization");
        }
    }
}

