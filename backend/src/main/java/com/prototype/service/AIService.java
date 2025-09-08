package com.prototype.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.prototype.entity.Ticket;
import com.prototype.entity.TicketMessage;
import com.prototype.repository.TicketRepository;
import com.prototype.repository.TicketMessageRepository;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

@Service
public class AIService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TicketMessageRepository messageRepository;
    
    public AIService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://localhost:11434")
            .build();
        this.objectMapper = new ObjectMapper();
        System.out.println("AIService initialized with Ollama integration");
    }
    
    /**
     * Analyze sentiment of a text message using Stanford CoreNLP
     */
    public SentimentAnalysisResult analyzeSentiment(String text) {
        try {
            // Initialize Stanford CoreNLP if needed
            if (sentimentPipeline == null) {
                initializeSentimentPipeline();
            }
            
            // If pipeline still null after initialization, fall back to neutral
            if (sentimentPipeline == null) {
                System.err.println("Sentiment pipeline not available, returning neutral");
                return new SentimentAnalysisResult(0.0f, "NEUTRAL");
            }
            
            // Analyze sentiment
            Annotation annotation = new Annotation(text);
            sentimentPipeline.annotate(annotation);
            
            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            if (sentences == null || sentences.isEmpty()) {
                return new SentimentAnalysisResult(0.0f, "NEUTRAL");
            }
            
            // Calculate average sentiment across all sentences
            double totalSentiment = 0.0;
            int sentenceCount = 0;
            
            for (CoreMap sentence : sentences) {
                Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
                if (tree != null) {
                    int sentimentValue = RNNCoreAnnotations.getPredictedClass(tree);
                    // Convert Stanford's 0-4 scale to -1 to +1 scale
                    // 0=Very Negative, 1=Negative, 2=Neutral, 3=Positive, 4=Very Positive
                    double normalizedSentiment = (sentimentValue - 2.0) / 2.0; // Maps to -1, -0.5, 0, 0.5, 1
                    totalSentiment += normalizedSentiment;
                    sentenceCount++;
                }
            }
            
            if (sentenceCount == 0) {
                return new SentimentAnalysisResult(0.0f, "NEUTRAL");
            }
            
            float averageSentiment = (float) (totalSentiment / sentenceCount);
            String label = getSentimentLabel(averageSentiment);
            
            System.out.println("Analyzed sentiment for: \"" + text + "\" -> Score: " + averageSentiment + ", Label: " + label);
            return new SentimentAnalysisResult(averageSentiment, label);
            
        } catch (Exception e) {
            System.err.println("Error analyzing sentiment: " + e.getMessage());
            e.printStackTrace();
            return new SentimentAnalysisResult(0.0f, "NEUTRAL");
        }
    }
    
    private StanfordCoreNLP sentimentPipeline;
    
    private void initializeSentimentPipeline() {
        try {
            Properties props = new Properties();
            // Simplified configuration - let Stanford CoreNLP use default models
            props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
            // Don't specify model paths - let CoreNLP find them automatically
            sentimentPipeline = new StanfordCoreNLP(props);
            System.out.println("Stanford CoreNLP sentiment pipeline initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize sentiment pipeline: " + e.getMessage());
            e.printStackTrace();
            sentimentPipeline = null;
        }
    }
    
    private String getSentimentLabel(float score) {
        // Define sentiment boundaries
        if (score > 0.1) return "POSITIVE";    // Above 0.1 is positive
        if (score < -0.1) return "NEGATIVE";   // Below -0.1 is negative  
        return "NEUTRAL";                      // -0.1 to 0.1 is neutral
    }
    
    /**
     * Analyze overall conversation sentiment using recent-weighted approach
     * More recent messages have higher weight in the calculation
     */
    public SentimentAnalysisResult analyzeConversationSentiment(Long ticketId) {
        try {
            List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
            if (messages.isEmpty()) {
                return new SentimentAnalysisResult(0.0f, "NEUTRAL");
            }
            
            // Filter to only customer messages (including AI customer responses, excluding agent and system messages)
            List<TicketMessage> customerMessages = messages.stream()
                .filter(msg -> msg.getSenderType().toString().equals("USER"))
                .collect(Collectors.toList());
            
            if (customerMessages.isEmpty()) {
                return new SentimentAnalysisResult(0.0f, "NEUTRAL");
            }
            
            // Recent-weighted sentiment calculation
            double totalWeightedSentiment = 0.0;
            double totalWeight = 0.0;
            
            for (int i = 0; i < customerMessages.size(); i++) {
                TicketMessage message = customerMessages.get(i);
                SentimentAnalysisResult messageSentiment = analyzeSentiment(message.getMessage());
                
                // Weight calculation: more recent messages get higher weight
                // Latest message gets weight 1.0, previous gets 0.8, then 0.6, etc.
                double weight = Math.max(0.2, 1.0 - (customerMessages.size() - 1 - i) * 0.2);
                
                totalWeightedSentiment += messageSentiment.getScore() * weight;
                totalWeight += weight;
            }
            
            float conversationSentiment = (float) (totalWeightedSentiment / totalWeight);
            String label = getSentimentLabel(conversationSentiment);
            
            System.out.println("Conversation sentiment for ticket " + ticketId + ": " + conversationSentiment + " (" + label + ")");
            return new SentimentAnalysisResult(conversationSentiment, label);
            
        } catch (Exception e) {
            System.err.println("Error analyzing conversation sentiment: " + e.getMessage());
            return new SentimentAnalysisResult(0.0f, "NEUTRAL");
        }
    }
    
    /**
     * Generate AI response using Ollama - context determines the AI behavior
     */
    public String generateResponse(String message, String context) {
        // Check if this is a dashboard analysis request
        if ("Dashboard Analysis Request".equals(context) || "Custom Dashboard Query".equals(context)) {
            return generateDashboardAnalysisResponse(message);
        }
        // Otherwise, use the ticket context behavior
        return generateResponseWithTicketContext(message, context, null);
    }
    
    /**
     * Generate dashboard analysis response using Ollama
     */
    private String generateDashboardAnalysisResponse(String analysisPrompt) {
        try {
            // Create system prompt for dashboard analysis
            String systemPrompt = "You are an expert data analyst specializing in customer service metrics and business intelligence. " +
                "Analyze the provided dashboard data and provide clear, actionable insights. " +
                "Focus on trends, performance indicators, and business implications. " +
                "Be professional, concise, and provide specific recommendations based on the data. " +
                "Structure your response with key findings and practical insights. " +
                "Avoid technical jargon and explain metrics in business terms.";
            
            StringBuilder fullPrompt = new StringBuilder();
            fullPrompt.append(systemPrompt).append("\n\n");
            fullPrompt.append(analysisPrompt);
            
            // Create request body for Ollama
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama3.2:3b");
            requestBody.put("prompt", fullPrompt.toString());
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", 0.3,  // Lower temperature for more focused analysis
                "top_p", 0.8,
                "max_tokens", 300    // Allow longer responses for detailed analysis
            ));
            
            // Call Ollama API
            String response = webClient.post()
                .uri("/api/generate")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response);
            String aiResponse = jsonResponse.get("response").asText();
            
            // Clean up the response for dashboard analysis
            aiResponse = cleanDashboardResponse(aiResponse);
            
            System.out.println("Generated dashboard analysis response: " + aiResponse);
            return aiResponse;
            
        } catch (Exception e) {
            System.err.println("Failed to generate dashboard analysis response: " + e.getMessage());
            e.printStackTrace();
            // Fallback response
            return "I'm unable to analyze the dashboard data at the moment. Please ensure your metrics are properly configured and try again.";
        }
    }
    
    /**
     * Generate AI response with full ticket conversation context
     */
    public String generateResponseWithTicketContext(String message, String context, Long ticketId) {
        try {
            // Get conversation history if ticket ID is provided
            String conversationHistory = "";
            String ticketContext = "";
            
            if (ticketId != null) {
                Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
                if (ticketOpt.isPresent()) {
                    Ticket ticket = ticketOpt.get();
                    
                    // Build ticket context
                    ticketContext = String.format("Ticket #%d: %s - %s (Status: %s, Priority: %s)", 
                        ticket.getId(), ticket.getSubject(), ticket.getDescription(),
                        ticket.getStatus(), ticket.getPriority());
                    
                    // Get conversation history (last 10 messages for context)
                    List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
                    if (!messages.isEmpty()) {
                        conversationHistory = buildConversationHistory(messages);
                    }
                }
            }
            
            // Create the prompt with conversation memory
            String systemPrompt = "You are a customer who has opened a support ticket and is communicating with a support agent. " +
                "Respond like a typical customer would - be brief, direct, and to the point. Most customer responses are 1-2 sentences. " +
                "Don't be overly polite or verbose. Ask short questions, give quick answers, show appreciation briefly. " +
                "Use casual but respectful language. Keep responses under 30 words when possible. " +
                "Don't put your response in quotes - just respond directly and naturally.";
            
            StringBuilder fullPrompt = new StringBuilder();
            fullPrompt.append(systemPrompt).append("\n\n");
            
            if (!ticketContext.isEmpty()) {
                fullPrompt.append("TICKET CONTEXT:\n").append(ticketContext).append("\n\n");
            }
            
            if (!conversationHistory.isEmpty()) {
                fullPrompt.append("CONVERSATION HISTORY:\n").append(conversationHistory).append("\n\n");
            }
            
            fullPrompt.append("LATEST SUPPORT AGENT MESSAGE: ").append(message).append("\n\n");
            fullPrompt.append("Your response as the customer:");
            
            // Create request body for Ollama
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama3.2:3b");
            requestBody.put("prompt", fullPrompt.toString());
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", 0.7,
                "top_p", 0.9,
                "max_tokens", 80
            ));
            
            // Call Ollama API
            String response = webClient.post()
                .uri("/api/generate")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response);
            String aiResponse = jsonResponse.get("response").asText();
            
            // Clean up the response
            aiResponse = cleanResponse(aiResponse);
            
            System.out.println("Generated AI response with conversation context: " + aiResponse);
            return aiResponse;
            
        } catch (Exception e) {
            System.err.println("Failed to generate AI response: " + e.getMessage());
            e.printStackTrace();
            // Fallback response
            return "I'm here to help! Could you tell me more about what you need assistance with?";
        }
    }
    
    /**
     * Build conversation history string from messages
     */
    private String buildConversationHistory(List<TicketMessage> messages) {
        // Get last 8 messages to keep context manageable but comprehensive
        List<TicketMessage> recentMessages = messages.size() > 8 ? 
            messages.subList(messages.size() - 8, messages.size()) : messages;
            
                    return recentMessages.stream()
            .map(msg -> {
                String sender;
                if (msg.getSenderType().toString().equals("USER")) {
                    // Check if it's an AI customer message
                    if (msg.getSenderName() != null && msg.getSenderName().contains("Customer (AI)")) {
                        sender = "You (Customer)";
                    } else {
                        sender = "Customer";
                    }
                } else if (msg.getSenderType().toString().equals("AGENT")) {
                    sender = "Support Agent";
                } else {
                    sender = "System";
                }
                return String.format("%s: %s", sender, msg.getMessage());
            })
            .collect(Collectors.joining("\n"));
    }
    
    private String cleanResponse(String response) {
        // Remove any unwanted prefixes or artifacts
        response = response.trim();
        
        // Remove common AI response prefixes
        if (response.startsWith("Customer service representative:")) {
            response = response.substring("Customer service representative:".length()).trim();
        }
        if (response.startsWith("Response:")) {
            response = response.substring("Response:".length()).trim();
        }
        if (response.startsWith("Customer:")) {
            response = response.substring("Customer:".length()).trim();
        }
        
        // Remove surrounding quotes if they wrap the entire response
        if ((response.startsWith("\"") && response.endsWith("\"")) || 
            (response.startsWith("'") && response.endsWith("'"))) {
            response = response.substring(1, response.length() - 1).trim();
        }
        
        // Remove any "heya" at the beginning - replace with more natural greetings
        if (response.toLowerCase().startsWith("heya")) {
            response = response.substring(4).trim();
            if (response.startsWith(",") || response.startsWith("!")) {
                response = response.substring(1).trim();
            }
            // Add a more natural greeting if the response now starts abruptly
            if (!response.toLowerCase().startsWith("hi") && !response.toLowerCase().startsWith("hello") && 
                !response.toLowerCase().startsWith("thanks") && !response.toLowerCase().startsWith("thank")) {
                response = "Hi, " + response;
            }
        }
        
        return response;
    }
    
    private String cleanDashboardResponse(String response) {
        // Clean up dashboard analysis response
        response = response.trim();
        
        // Remove common AI response prefixes for analysis
        if (response.startsWith("Based on the dashboard data,")) {
            response = response.substring("Based on the dashboard data,".length()).trim();
        }
        if (response.startsWith("Analysis:")) {
            response = response.substring("Analysis:".length()).trim();
        }
        if (response.startsWith("Response:")) {
            response = response.substring("Response:".length()).trim();
        }
        
        // Remove surrounding quotes
        if ((response.startsWith("\"") && response.endsWith("\"")) || 
            (response.startsWith("'") && response.endsWith("'"))) {
            response = response.substring(1, response.length() - 1).trim();
        }
        
        // Ensure the response starts with a capital letter
        if (!response.isEmpty() && Character.isLowerCase(response.charAt(0))) {
            response = Character.toUpperCase(response.charAt(0)) + response.substring(1);
        }
        
        return response;
    }
    
    /**
     * Simple sentiment analysis result class
     */
    public static class SentimentAnalysisResult {
        private final float score;
        private final String label;
        
        public SentimentAnalysisResult(float score, String label) {
            this.score = score;
            this.label = label;
        }
        
        public float getScore() {
            return score;
        }
        
        public String getLabel() {
            return label;
        }
    }
}