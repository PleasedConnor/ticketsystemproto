package com.prototype.service;

import com.prototype.entity.ChatbotAction;
import com.prototype.repository.ChatbotActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatbotActionService {
    
    @Autowired
    private ChatbotActionRepository actionRepository;
    
    public List<ChatbotAction> getAllActions() {
        return actionRepository.findAllByOrderByIdAsc();
    }
    
    public List<ChatbotAction> getActiveActions() {
        return actionRepository.findByIsActiveTrueOrderByIdAsc();
    }
    
    @Transactional(readOnly = true)
    public ChatbotAction getActionById(Long id) {
        return actionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Chatbot action not found with id: " + id));
    }
    
    @Transactional(readOnly = true)
    public ChatbotAction getActionByKey(String actionKey) {
        return actionRepository.findByActionKey(actionKey)
            .orElseThrow(() -> new RuntimeException("Chatbot action not found with key: " + actionKey));
    }
    
    @Transactional
    public ChatbotAction createAction(ChatbotAction action) {
        // Validate action key is unique
        if (actionRepository.findByActionKey(action.getActionKey()).isPresent()) {
            throw new RuntimeException("Action key already exists: " + action.getActionKey());
        }
        return actionRepository.save(action);
    }
    
    @Transactional
    public ChatbotAction updateAction(Long id, ChatbotAction action) {
        ChatbotAction existing = getActionById(id);
        
        // If action key is being changed, check it's unique
        if (!existing.getActionKey().equals(action.getActionKey())) {
            if (actionRepository.findByActionKey(action.getActionKey()).isPresent()) {
                throw new RuntimeException("Action key already exists: " + action.getActionKey());
            }
        }
        
        existing.setName(action.getName());
        existing.setActionKey(action.getActionKey());
        existing.setComponentType(action.getComponentType());
        existing.setConfig(action.getConfig());
        existing.setDescription(action.getDescription());
        existing.setIsActive(action.getIsActive());
        
        return actionRepository.save(existing);
    }
    
    @Transactional
    public void deleteAction(Long id) {
        actionRepository.deleteById(id);
    }
}

