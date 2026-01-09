package com.prototype.service;

import com.prototype.entity.*;
import com.prototype.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimulationService {
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TicketMessageRepository messageRepository;
    
    @Autowired
    private SimulationResultRepository simulationResultRepository;
    
    @Autowired
    private AIService aiService;
    
    /**
     * Get tickets for simulation based on selection criteria
     */
    public List<Ticket> getTicketsForSimulation(String selectionMode, List<Long> ticketIds, 
                                                 List<String> categories, Integer maxTickets) {
        List<Ticket> allTickets = ticketRepository.findAll();
        
        if ("specific".equals(selectionMode)) {
            if (ticketIds != null && !ticketIds.isEmpty()) {
                return allTickets.stream()
                    .filter(t -> ticketIds.contains(t.getId()))
                    .limit(1000)
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } else {
            // Category-based selection
            List<Ticket> candidates = allTickets;
            
            if (categories != null && !categories.isEmpty()) {
                candidates = candidates.stream()
                    .filter(t -> t.getCategory() != null && categories.contains(t.getCategory().toString()))
                    .collect(Collectors.toList());
            }
            
            // Random sample if maxTickets is specified
            if (maxTickets != null && maxTickets > 0 && candidates.size() > maxTickets) {
                Collections.shuffle(candidates);
                return candidates.stream().limit(maxTickets).collect(Collectors.toList());
            }
            
            return candidates;
        }
    }
    
    /**
     * Simulate AI response for a ticket
     */
    public SimulationResult simulateTicket(Ticket ticket, String modelName) {
        // Get the last agent message to use as context
        List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
        List<TicketMessage> agentMessages = messages.stream()
            .filter(m -> m.getSenderType() == SenderType.AGENT)
            .collect(Collectors.toList());
        
        // Get the last customer message to understand the query
        List<TicketMessage> customerMessages = messages.stream()
            .filter(m -> m.getSenderType() == SenderType.USER)
            .collect(Collectors.toList());
        
        String humanReply = agentMessages.isEmpty() ? 
            "No agent response available" : 
            agentMessages.get(agentMessages.size() - 1).getMessage();
        
        // Generate AI agent response using the AIService
        // The AI should act as a customer service agent responding to the customer
        String context = ticket.getDescription() != null ? ticket.getDescription() : ticket.getSubject();
        String lastCustomerMessage = customerMessages.isEmpty() ? 
            context : 
            customerMessages.get(customerMessages.size() - 1).getMessage();
        
        // Use the new method that generates agent responses (not customer responses)
        String aiReply = aiService.generateAgentResponseForSimulation(
            lastCustomerMessage, 
            context, 
            ticket.getId()
        );
        
        // Determine AI self-reflection (simplified logic based on reply quality)
        boolean aiCorrect = evaluateAIResponse(aiReply, ticket);
        double confidence = calculateConfidence(aiReply, modelName);
        String aiNotes = generateAINotes(aiCorrect, ticket);
        
        SimulationResult result = new SimulationResult();
        result.setTicketId(ticket.getId());
        result.setTicketSubject(ticket.getSubject());
        result.setHumanReply(humanReply);
        result.setAiReply(aiReply);
        result.setAiSelfReflectionCorrect(aiCorrect);
        result.setAiSelfReflectionConfidence(confidence);
        result.setAiNotes(aiNotes);
        result.setModelName(modelName);
        
        return result;
    }
    
    /**
     * Evaluate if AI response is correct (simplified heuristic)
     */
    private boolean evaluateAIResponse(String aiReply, Ticket ticket) {
        if (aiReply == null || aiReply.trim().isEmpty()) {
            return false;
        }
        
        String replyLower = aiReply.toLowerCase();
        
        // Check if response is too short (likely incomplete)
        if (aiReply.length() < 20) {
            return false;
        }
        
        // Check for common helpful phrases
        boolean hasHelpfulPhrases = replyLower.contains("can help") || 
                                   replyLower.contains("let me") ||
                                   replyLower.contains("i'll") ||
                                   replyLower.contains("please") ||
                                   replyLower.contains("check") ||
                                   replyLower.contains("verify");
        
        // Check for vague/unhelpful phrases
        boolean hasVaguePhrases = replyLower.contains("just wait") ||
                                 replyLower.contains("always") ||
                                 replyLower.contains("never") ||
                                 (replyLower.contains("sorry") && !replyLower.contains("can"));
        
        // Ticket priority-based evaluation (note: priority is different from category)
        if (ticket.getPriority() == TicketPriority.URGENT || 
            ticket.getPriority() == TicketPriority.HIGH) {
            // Higher standards for urgent/high priority tickets
            return hasHelpfulPhrases && !hasVaguePhrases && aiReply.length() > 50;
        }
        
        return hasHelpfulPhrases && !hasVaguePhrases;
    }
    
    /**
     * Calculate confidence score (0.0 to 1.0)
     */
    private double calculateConfidence(String aiReply, String modelName) {
        double base = 0.78;
        
        // Model adjustments
        if (modelName != null) {
            if (modelName.contains("Pro")) {
                base += 0.06;
            } else if (modelName.contains("Lite")) {
                base -= 0.06;
            }
        }
        
        // Reply quality adjustments
        if (aiReply != null) {
            if (aiReply.length() > 100) base += 0.05;
            if (aiReply.length() < 30) base -= 0.15;
            
            // Check for specific helpful patterns
            String lower = aiReply.toLowerCase();
            if (lower.contains("escalate") || lower.contains("check") || lower.contains("verify")) {
                base += 0.05;
            }
        }
        
        // Add some randomness
        base += (Math.random() - 0.5) * 0.2;
        
        // Clamp between 0.15 and 0.97
        return Math.max(0.15, Math.min(0.97, base));
    }
    
    /**
     * Generate AI self-reflection notes
     */
    private String generateAINotes(boolean correct, Ticket ticket) {
        if (correct) {
            return "Self-check: Response addresses the query with appropriate context and next steps.";
        } else {
            String priority = ticket.getPriority().toString().toLowerCase();
            return String.format("Self-check: Response may need improvement for %s priority ticket. " +
                               "Consider adding more specific guidance or escalation criteria.", priority);
        }
    }
    
    /**
     * Save simulation result
     */
    public SimulationResult saveResult(SimulationResult result) {
        return simulationResultRepository.save(result);
    }
    
    /**
     * Update simulation result with human review
     */
    public SimulationResult updateResult(Long resultId, SimulationResult.HumanMark humanMark, 
                                         String humanNotes, String idealResponse) {
        SimulationResult result = simulationResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Simulation result not found"));
        
        result.setHumanMark(humanMark);
        result.setHumanNotes(humanNotes);
        result.setIdealResponse(idealResponse);
        
        return simulationResultRepository.save(result);
    }
    
    /**
     * Get all results for a simulation run
     */
    public List<SimulationResult> getResultsByRunId(String runId) {
        return simulationResultRepository.findBySimulationRunId(runId);
    }
    
    /**
     * Get result by ticket ID and run ID
     */
    public Optional<SimulationResult> getResult(Long ticketId, String runId) {
        return simulationResultRepository.findByTicketIdAndSimulationRunId(ticketId, runId);
    }
}

