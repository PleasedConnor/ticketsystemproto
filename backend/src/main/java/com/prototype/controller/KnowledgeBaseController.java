package com.prototype.controller;

import com.prototype.entity.Article;
import com.prototype.entity.Category;
import com.prototype.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge-base")
@CrossOrigin(origins = "*")
public class KnowledgeBaseController {
    
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    
    // Category endpoints
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = knowledgeBaseService.getAllCategories();
        System.out.println("Returning " + categories.size() + " categories");
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/categories/{id}")
    public ResponseEntity<Category> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeBaseService.getCategoryById(id));
    }
    
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        try {
            System.out.println("Received category creation request: " + category.getName());
            Category created = knowledgeBaseService.createCategory(category);
            System.out.println("Category created successfully with ID: " + created.getId());
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in createCategory controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create category: " + e.getMessage(), "message", e.getMessage()));
        }
    }
    
    @PutMapping("/categories/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return ResponseEntity.ok(knowledgeBaseService.updateCategory(id, category));
    }
    
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        knowledgeBaseService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
    
    // Article endpoints
    @GetMapping("/articles")
    public ResponseEntity<List<Article>> getAllArticles() {
        List<Article> articles = knowledgeBaseService.getAllArticles();
        System.out.println("Returning " + articles.size() + " articles");
        return ResponseEntity.ok(articles);
    }
    
    @GetMapping("/articles/active")
    public ResponseEntity<List<Article>> getActiveArticles() {
        return ResponseEntity.ok(knowledgeBaseService.getActiveArticles());
    }
    
    @GetMapping("/articles/category/{categoryId}")
    public ResponseEntity<List<Article>> getArticlesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(knowledgeBaseService.getArticlesByCategory(categoryId));
    }
    
    @GetMapping("/articles/{id}")
    public ResponseEntity<Article> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeBaseService.getArticleById(id));
    }
    
    @PostMapping("/articles")
    public ResponseEntity<Article> createArticle(@RequestBody Article article) {
        try {
            return ResponseEntity.ok(knowledgeBaseService.createArticle(article));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/articles/{id}")
    public ResponseEntity<Article> updateArticle(@PathVariable Long id, @RequestBody Article article) {
        return ResponseEntity.ok(knowledgeBaseService.updateArticle(id, article));
    }
    
    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        knowledgeBaseService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
    
    // Search endpoint for RAG
    @PostMapping("/search")
    public ResponseEntity<List<Article>> searchArticles(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(knowledgeBaseService.searchArticles(query));
    }
}

