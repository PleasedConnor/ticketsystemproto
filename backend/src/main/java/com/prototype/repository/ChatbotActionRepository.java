package com.prototype.repository;

import com.prototype.entity.ChatbotAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatbotActionRepository extends JpaRepository<ChatbotAction, Long> {
    
    Optional<ChatbotAction> findByActionKey(String actionKey);
    
    List<ChatbotAction> findByIsActiveTrueOrderByIdAsc();
    
    List<ChatbotAction> findAllByOrderByIdAsc();
}

