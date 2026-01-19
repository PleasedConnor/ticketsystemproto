package com.prototype.repository;

import com.prototype.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByCategoryId(Long categoryId);
    List<Article> findByIsActiveTrue();
    List<Article> findByCategoryIdAndIsActiveTrue(Long categoryId);
    
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.category")
    List<Article> findAllWithCategory();
}

