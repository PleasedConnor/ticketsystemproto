package com.prototype.controller;

import com.prototype.entity.Ticket;
import com.prototype.entity.TicketMessage;
import com.prototype.entity.SenderType;
import com.prototype.service.AIService;
import com.prototype.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "*")
public class AIController {
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private TicketService ticketService;
    
    /**
     * Analyze sentiment of a message
     */
    @PostMapping("/sentiment")
    public ResponseEntity<AIService.SentimentAnalysisResult> analyzeSentiment(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        AIService.SentimentAnalysisResult result = aiService.analyzeSentiment(text);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get conversation sentiment for a ticket
     */
    @GetMapping("/tickets/{ticketId}/sentiment")
    public ResponseEntity<AIService.SentimentAnalysisResult> getConversationSentiment(@PathVariable Long ticketId) {
        AIService.SentimentAnalysisResult result = aiService.analyzeConversationSentiment(ticketId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Generate AI response and optionally add it to the ticket
     */
    @PostMapping("/tickets/{ticketId}/generate-response")
    public ResponseEntity<AIResponseResult> generateResponse(
            @PathVariable Long ticketId,
            @RequestBody GenerateResponseRequest request) {
        
        // Get the ticket for context
        Optional<Ticket> ticketOpt = ticketService.getTicketById(ticketId);
        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Ticket ticket = ticketOpt.get();
        String context = ticket.getSubject() + ": " + ticket.getDescription();
        
        // Generate AI response with conversation memory using AIService
        String aiResponse = aiService.generateResponseWithTicketContext(request.getMessage(), context, ticketId);
        
        // Optionally add the AI response as a message to the ticket
        TicketMessage aiMessage = null;
        if (request.isAddToTicket()) {
            aiMessage = new TicketMessage();
            aiMessage.setMessage(aiResponse);
            aiMessage.setSenderType(SenderType.USER);
            aiMessage.setSenderName("Customer (AI)");
            aiMessage.setTicket(ticket);
            
            aiMessage = ticketService.addMessage(aiMessage);
        }
        
        return ResponseEntity.ok(new AIResponseResult(aiResponse, aiMessage));
    }
    
    // Request/Response classes
    public static class GenerateResponseRequest {
        private String message;
        private boolean addToTicket = false;
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public boolean isAddToTicket() { return addToTicket; }
        public void setAddToTicket(boolean addToTicket) { this.addToTicket = addToTicket; }
    }
    
    public static class AIResponseResult {
        private String response;
        private TicketMessage message;
        
        public AIResponseResult(String response, TicketMessage message) {
            this.response = response;
            this.message = message;
        }
        
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        
        public TicketMessage getMessage() { return message; }
        public void setMessage(TicketMessage message) { this.message = message; }
    }
}
