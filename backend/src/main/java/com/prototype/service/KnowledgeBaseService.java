package com.prototype.service;

import com.prototype.entity.Article;
import com.prototype.entity.Category;
import com.prototype.repository.ArticleRepository;
import com.prototype.repository.CategoryRepository;
import com.prototype.rag.chunking.DocumentChunk;
import com.prototype.rag.chunking.DocumentChunker;
import com.prototype.rag.embedding.EmbeddingService;
import com.prototype.rag.storage.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeBaseService {
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired(required = false)
    private EmbeddingService embeddingService;
    
    @Autowired(required = false)
    private VectorStore vectorStore;
    
    private final DocumentChunker documentChunker = new DocumentChunker();
    
    // Category methods
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        System.out.println("Found " + categories.size() + " categories in database");
        for (Category cat : categories) {
            System.out.println("  - Category: " + cat.getName() + " (ID: " + cat.getId() + ")");
        }
        return categories;
    }
    
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }
    
    @Transactional
    public Category createCategory(Category category) {
        // Validate required fields
        if (category == null) {
            throw new IllegalArgumentException("Category object is required");
        }
        
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }
        
        String categoryName = category.getName().trim();
        
        // Check if category with same name already exists (case-insensitive)
        List<Category> allCategories = categoryRepository.findAll();
        for (Category existing : allCategories) {
            if (existing.getName() != null && existing.getName().trim().equalsIgnoreCase(categoryName)) {
                throw new IllegalArgumentException("Category with name '" + categoryName + "' already exists");
            }
        }
        
        // Create a new category object to avoid any entity state issues
        Category newCategory = new Category();
        newCategory.setName(categoryName);
        newCategory.setDescription(category.getDescription() != null ? category.getDescription().trim() : null);
        
        // Timestamps are set in constructor, but ensure updatedAt is current
        newCategory.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Save the category
        try {
            Category saved = categoryRepository.save(newCategory);
            categoryRepository.flush(); // Force immediate write
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle unique constraint violation
            throw new IllegalArgumentException("Category with name '" + categoryName + "' already exists", e);
        } catch (Exception e) {
            System.err.println("Error creating category: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create category: " + e.getMessage(), e);
        }
    }
    
    public Category updateCategory(Long id, Category category) {
        Category existing = getCategoryById(id);
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        return categoryRepository.save(existing);
    }
    
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
    
    // Article methods
    @Transactional(readOnly = true)
    public List<Article> getAllArticles() {
        // Use fetch join to eagerly load categories and avoid LazyInitializationException
        List<Article> articles = articleRepository.findAllWithCategory();
        System.out.println("Found " + articles.size() + " articles in database");
        // Log article details (category should be loaded now)
        for (Article article : articles) {
            Category cat = article.getCategory();
            System.out.println("  - Article: " + article.getTitle() + 
                " (ID: " + article.getId() + 
                ", Category: " + (cat != null ? cat.getName() + " (ID: " + cat.getId() + ")" : "none") + ")");
        }
        return articles;
    }
    
    @Transactional(readOnly = true)
    public List<Article> getActiveArticles() {
        List<Article> articles = articleRepository.findByIsActiveTrue();
        System.out.println("getActiveArticles: Found " + articles.size() + " active articles");
        // Force load categories for active articles
        for (Article article : articles) {
            System.out.println("  - Active article: '" + article.getTitle() + "' (ID: " + article.getId() + ")");
            if (article.getCategory() != null) {
                article.getCategory().getName(); // Force load
            }
        }
        return articles;
    }
    
    @Transactional(readOnly = true)
    public List<Article> getArticlesByCategory(Long categoryId) {
        List<Article> articles = articleRepository.findByCategoryId(categoryId);
        // Categories are already loaded since we're filtering by categoryId
        return articles;
    }
    
    @Transactional(readOnly = true)
    public List<Article> getActiveArticlesByCategory(Long categoryId) {
        return articleRepository.findByCategoryIdAndIsActiveTrue(categoryId);
    }
    
    @Transactional(readOnly = true)
    public Article getArticleById(Long id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));
        // Force load category if it exists
        if (article.getCategory() != null) {
            article.getCategory().getName(); // Force load
        }
        return article;
    }
    
    @Transactional
    public Article createArticle(Article article) {
        // Validate required fields
        if (article.getTitle() == null || article.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Article title is required");
        }
        if (article.getContent() == null || article.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Article content is required");
        }
        
        // Handle category relationship
        if (article.getCategory() != null && article.getCategory().getId() != null) {
            try {
                Category category = getCategoryById(article.getCategory().getId());
                article.setCategory(category);
            } catch (RuntimeException e) {
                // Category not found, set to null
                article.setCategory(null);
            }
        } else {
            article.setCategory(null);
        }
        
        // Ensure isActive is set
        if (article.getIsActive() == null) {
            article.setIsActive(true);
        }
        
        // Ensure timestamps are set
        if (article.getCreatedAt() == null) {
            article.setCreatedAt(java.time.LocalDateTime.now());
        }
        article.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Use saveAndFlush to force immediate write to database
        Article saved = articleRepository.saveAndFlush(article);
        
        // Index the article in the vector store for RAG
        indexArticleForRAG(saved);
        
        return saved;
    }
    
    /**
     * Index an article in the vector store for semantic search
     */
    private void indexArticleForRAG(Article article) {
        if (embeddingService == null || vectorStore == null) {
            // RAG components not available, skip indexing
            return;
        }
        
        try {
            // Remove old chunks for this article
            vectorStore.removeChunksByArticleId(article.getId());
            
            // Chunk the article
            String categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
            List<DocumentChunk> chunks = documentChunker.chunkArticle(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                categoryName
            );
            
            if (chunks.isEmpty()) {
                return;
            }
            
            System.out.println("Indexing article '" + article.getTitle() + "' into vector store (" + chunks.size() + " chunks)");
            System.out.println("Original article content length: " + (article.getContent() != null ? article.getContent().length() : 0));
            
            // Generate embeddings for each chunk
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                System.out.println("  Chunk " + (i+1) + "/" + chunks.size() + " - Content length: " + 
                    (chunk.getContent() != null ? chunk.getContent().length() : 0) + 
                    " chars, Preview: " + (chunk.getContent() != null && chunk.getContent().length() > 50 ? 
                    chunk.getContent().substring(0, Math.min(50, chunk.getContent().length())) + "..." : chunk.getContent()));
                
                try {
                    List<Float> embedding = embeddingService.generateEmbedding(chunk.getContent());
                    chunk.setEmbedding(embedding);
                    vectorStore.addChunk(chunk);
                    System.out.println("    ✓ Embedded and stored chunk " + (i+1));
                } catch (Exception e) {
                    System.err.println("    ✗ Failed to generate embedding for chunk " + (i+1) + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("Successfully indexed " + chunks.size() + " chunks for article '" + article.getTitle() + "'");
        } catch (Exception e) {
            System.err.println("Failed to index article for RAG: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Re-index all active articles in the vector store
     * Useful for initial setup or after RAG system initialization
     */
    @Transactional
    public void reindexAllArticles() {
        if (embeddingService == null || vectorStore == null) {
            System.out.println("RAG components not available, skipping reindexing");
            return;
        }
        
        System.out.println("Reindexing all active articles...");
        List<Article> articles = getActiveArticles();
        
        for (Article article : articles) {
            indexArticleForRAG(article);
        }
        
        System.out.println("Reindexing complete. Total chunks in vector store: " + vectorStore.getChunkCount());
    }
    
    public Article updateArticle(Long id, Article article) {
        Article existing = getArticleById(id);
        existing.setTitle(article.getTitle());
        existing.setContent(article.getContent());
        existing.setIsActive(article.getIsActive());
        
        if (article.getCategory() != null && article.getCategory().getId() != null) {
            Category category = getCategoryById(article.getCategory().getId());
            existing.setCategory(category);
        } else {
            existing.setCategory(null);
        }
        
        Article saved = articleRepository.save(existing);
        
        // Re-index the article in the vector store if it's active
        if (saved.getIsActive()) {
            indexArticleForRAG(saved);
        } else {
            // Remove from vector store if article is deactivated
            if (vectorStore != null) {
                vectorStore.removeChunksByArticleId(saved.getId());
            }
        }
        
        return saved;
    }
    
    public void deleteArticle(Long id) {
        // Remove from vector store before deleting
        if (vectorStore != null) {
            vectorStore.removeChunksByArticleId(id);
        }
        articleRepository.deleteById(id);
    }
    
    /**
     * Search articles by content (for RAG)
     * Enhanced text search with keyword matching and relevance scoring
     */
    @Transactional(readOnly = true)
    public List<Article> searchArticles(String query) {
        List<Article> allActive = getActiveArticles();
        if (allActive.isEmpty()) {
            System.out.println("No active articles found for search");
            return List.of();
        }
        
        String lowerQuery = query.toLowerCase().trim();
        System.out.println("Searching articles with query: '" + query + "'");
        
        // Extract keywords from query (split by spaces, remove punctuation, filter common words)
        String[] queryWords = lowerQuery.split("\\s+");
        java.util.Set<String> stopWords = java.util.Set.of("what", "is", "are", "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "from", "by", "as", "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they", "do", "does", "did", "can", "could", "will", "would", "should");
        java.util.Set<String> keywords = new java.util.HashSet<>();
        for (String word : queryWords) {
            // Remove punctuation and convert to lowercase
            String cleanWord = word.replaceAll("[^a-z0-9]", "").toLowerCase();
            if (cleanWord.length() > 2 && !stopWords.contains(cleanWord)) {
                keywords.add(cleanWord);
            }
        }
        
        System.out.println("Search keywords (filtered): " + keywords);
        System.out.println("Total active articles to search: " + allActive.size());
        
        // Score articles based on keyword matches
        java.util.List<java.util.Map.Entry<Article, Integer>> scoredArticles = new java.util.ArrayList<>();
        
        for (Article article : allActive) {
            int score = 0;
            String titleLower = article.getTitle().toLowerCase();
            // Strip HTML and normalize whitespace
            String contentLower = article.getContent().replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .toLowerCase()
                .trim();
            
            System.out.println("  Checking article: '" + article.getTitle() + "'");
            System.out.println("    Content preview: " + (contentLower.length() > 100 ? contentLower.substring(0, 100) + "..." : contentLower));
            
            // Check for exact phrase match (highest priority) - remove punctuation from query too
            String queryNoPunct = lowerQuery.replaceAll("[^a-z0-9\\s]", " ");
            if (contentLower.contains(queryNoPunct) || titleLower.contains(queryNoPunct)) {
                score += 100;
                System.out.println("    ✓ Exact phrase match (+100)");
            }
            
            // Check for individual keyword matches (case-insensitive, flexible matching)
            for (String keyword : keywords) {
                // Generate variations of the keyword (singular/plural, etc.)
                java.util.Set<String> keywordVariations = new java.util.HashSet<>();
                keywordVariations.add(keyword); // Original
                
                // Handle common plural/singular variations
                if (keyword.endsWith("s") && keyword.length() > 3) {
                    // Remove 's' for singular (e.g., "connors" -> "connor")
                    keywordVariations.add(keyword.substring(0, keyword.length() - 1));
                } else if (!keyword.endsWith("s")) {
                    // Add 's' for plural (e.g., "connor" -> "connors")
                    keywordVariations.add(keyword + "s");
                }
                
                boolean titleMatch = false;
                boolean contentMatch = false;
                
                // Try each variation
                for (String variation : keywordVariations) {
                    // Try exact word match first (with word boundaries)
                    String keywordPattern = "\\b" + java.util.regex.Pattern.quote(variation) + "\\b";
                    if (!titleMatch && titleLower.matches(".*" + keywordPattern + ".*")) {
                        titleMatch = true;
                        System.out.println("    ✓ Title keyword match: '" + variation + "' (from '" + keyword + "') (+10)");
                    }
                    if (!contentMatch && contentLower.matches(".*" + keywordPattern + ".*")) {
                        contentMatch = true;
                        System.out.println("    ✓ Content keyword match: '" + variation + "' (from '" + keyword + "') (+5)");
                    }
                }
                
                // If still no match, try partial match (substring) as last resort
                if (!titleMatch && !contentMatch) {
                    for (String variation : keywordVariations) {
                        if (titleLower.contains(variation)) {
                            titleMatch = true;
                            System.out.println("    ⚠ Title partial match: '" + variation + "' (from '" + keyword + "') (+10)");
                        }
                        if (contentLower.contains(variation)) {
                            contentMatch = true;
                            System.out.println("    ⚠ Content partial match: '" + variation + "' (from '" + keyword + "') (+5)");
                        }
                    }
                }
                
                if (titleMatch) {
                    score += 10; // Title matches are more important
                }
                if (contentMatch) {
                    score += 5; // Content matches
                }
            }
            
            if (score > 0) {
                scoredArticles.add(new java.util.AbstractMap.SimpleEntry<>(article, score));
                System.out.println("    → Final score: " + score);
            } else {
                System.out.println("    → No matches (score: 0)");
            }
        }
        
        // Sort by score (descending) and return top results
        scoredArticles.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        List<Article> results = scoredArticles.stream()
            .map(java.util.Map.Entry::getKey)
            .limit(5) // Return top 5 most relevant articles
            .toList();
        
        System.out.println("Found " + results.size() + " relevant articles");
        return results;
    }
}

