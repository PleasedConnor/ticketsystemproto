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
import com.prototype.service.AIConfigurationService;
import com.prototype.service.KnowledgeBaseService;
import com.prototype.rag.pipeline.RAGPipeline;

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
    
    @Autowired
    private AIConfigurationService configurationService;
    
    @Autowired(required = false)
    private com.prototype.service.AIRuleService airuleService;
    
    @Autowired(required = false)
    private com.prototype.service.ChatbotActionService chatbotActionService;
    
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    
    @Autowired(required = false)
    private RAGPipeline ragPipeline;
    
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
        // Define sentiment boundaries - widened neutral range to account for factual problem statements
        // that aren't actually negative in tone (e.g., "I'm missing money" said politely)
        if (score > 0.2) return "POSITIVE";    // Above 0.2 is positive
        if (score < -0.2) return "NEGATIVE";   // Below -0.2 is negative  
        return "NEUTRAL";                      // -0.2 to 0.2 is neutral
    }
    
    /**
     * Adjust sentiment score for polite/factual language
     * If a message mentions problems but uses polite language, reduce the negative bias
     */
    private float adjustSentimentForPoliteness(String message, float originalScore) {
        if (originalScore >= 0) {
            // Already positive or neutral, no adjustment needed
            return originalScore;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Polite language indicators
        boolean hasPoliteLanguage = lowerMessage.contains("please") || 
                                   lowerMessage.contains("thank") ||
                                   lowerMessage.contains("appreciate") ||
                                   lowerMessage.contains("would be great") ||
                                   lowerMessage.contains("could you") ||
                                   lowerMessage.contains("would you") ||
                                   lowerMessage.contains("if possible") ||
                                   lowerMessage.contains("when you can");
        
        // Factual/problem-reporting language (not angry/complaining)
        boolean isFactualReport = (lowerMessage.contains("missing") || 
                                  lowerMessage.contains("issue") || 
                                  lowerMessage.contains("problem") ||
                                  lowerMessage.contains("not working") ||
                                  lowerMessage.contains("don't see")) &&
                                  !lowerMessage.contains("angry") &&
                                  !lowerMessage.contains("frustrated") &&
                                  !lowerMessage.contains("terrible") &&
                                  !lowerMessage.contains("awful") &&
                                  !lowerMessage.contains("horrible") &&
                                  !lowerMessage.contains("worst") &&
                                  !lowerMessage.contains("hate") &&
                                  !lowerMessage.contains("disgusted");
        
        // If message is polite or factual (not angry), reduce negative bias
        if (hasPoliteLanguage || isFactualReport) {
            // Move negative scores closer to neutral (reduce by 30-50%)
            float adjustment = originalScore * 0.4f; // Reduce negative impact by 60%
            return Math.max(originalScore, adjustment);
        }
        
        return originalScore;
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
                
                // Adjust sentiment score for polite/factual language
                // If message mentions problems but uses polite language, reduce negative bias
                float adjustedScore = adjustSentimentForPoliteness(message.getMessage(), messageSentiment.getScore());
                
                // Weight calculation: more recent messages get higher weight
                // Latest message gets weight 1.0, previous gets 0.8, then 0.6, etc.
                // Reduced weight decay to be less aggressive (0.15 instead of 0.2)
                double weight = Math.max(0.3, 1.0 - (customerMessages.size() - 1 - i) * 0.15);
                
                totalWeightedSentiment += adjustedScore * weight;
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
     * Generate AI agent response for chatbot - acts as a customer service agent
     * @param isActionTriggered If true, suppress actions in response to prevent loops
     */
    public String generateAgentResponseForChatbot(String customerMessage, String conversationHistory, boolean isActionTriggered) {
        try {
            // Get AI configurations - try new rule-based system first, fall back to old system
            String configurationContext = "";
            try {
                if (airuleService != null) {
                    configurationContext = airuleService.getAllRulesAsPrompt(null);
                    System.out.println("=== LOADED AI CONFIGURATION ===");
                    System.out.println("Configuration length: " + (configurationContext != null ? configurationContext.length() : 0));
                    System.out.println("Configuration preview: " + (configurationContext != null && configurationContext.length() > 200 ? 
                        configurationContext.substring(0, 200) + "..." : configurationContext));
                    System.out.println("=== END CONFIGURATION ===");
                } else {
                    configurationContext = configurationService.getAllActiveConfigurationsAsPrompt();
                }
            } catch (Exception e) {
                System.err.println("Failed to load AI configurations: " + e.getMessage());
                e.printStackTrace();
                // Fallback to old system
                try {
                    configurationContext = configurationService.getAllActiveConfigurationsAsPrompt();
                } catch (Exception e2) {
                    System.err.println("Failed to load AI configurations (fallback): " + e2.getMessage());
                }
            }
            
            // Determine if this is the first message
            boolean isFirstMessage = (conversationHistory == null || conversationHistory.trim().isEmpty());
            
            StringBuilder fullPrompt = new StringBuilder();
            
            // MINIMAL PROMPT STRUCTURE - Configuration is the ONLY source of behavior
            // Process configuration to handle variables - NO hardcoded behavior
            String processedConfig = configurationContext;
            if (processedConfig != null && !processedConfig.trim().isEmpty()) {
                // Replace {{user.name}} with placeholder so configuration can reference it
                // The configuration rule itself will instruct what to do with this placeholder
                processedConfig = processedConfig.replaceAll("\\{\\{user\\.name\\}\\}", 
                    isFirstMessage ? "[ASK_USER_FOR_NAME]" : "[CHECK_CONVERSATION_FOR_NAME_OR_ASK]");
            }
            
            // Build the simplest possible prompt
            fullPrompt.append("You are a customer service agent.\n\n");
            
            // Add configuration rules FIRST - this is the ONLY behavior definition
            // NO hardcoded instructions - the configuration itself contains all behavior
            if (processedConfig != null && !processedConfig.trim().isEmpty()) {
                fullPrompt.append("=== CONFIGURATION RULES (MANDATORY - FOLLOW EXACTLY) ===\n");
                fullPrompt.append("The following rules are defined in the platform configuration. ");
                fullPrompt.append("These are MANDATORY instructions that you must follow exactly:\n\n");
                fullPrompt.append(processedConfig).append("\n");
                fullPrompt.append("=== END CONFIGURATION RULES ===\n\n");
                
                // Only explain placeholders if they exist in the config - no hardcoded behavior
                if (processedConfig.contains("ASK_USER_FOR_NAME") || processedConfig.contains("CHECK_CONVERSATION_FOR_NAME_OR_ASK")) {
                    fullPrompt.append("NOTE: [ASK_USER_FOR_NAME] means ask the user for their name. ");
                    fullPrompt.append("[CHECK_CONVERSATION_FOR_NAME_OR_ASK] means check conversation history for a name, and if not found, ask for it.\n\n");
                }
            }
            
            // Add available actions context (after configuration so rules can reference them)
            if (chatbotActionService != null) {
                try {
                    List<com.prototype.entity.ChatbotAction> availableActions = chatbotActionService.getActiveActions();
                    if (!availableActions.isEmpty()) {
                        fullPrompt.append("=== AVAILABLE ACTIONS ===\n");
                        fullPrompt.append("You can display interactive components by including action placeholders in your response.\n");
                        fullPrompt.append("Available actions:\n");
                        for (com.prototype.entity.ChatbotAction action : availableActions) {
                            fullPrompt.append("- {action_").append(action.getActionKey()).append("} - ").append(action.getName());
                            if (action.getDescription() != null && !action.getDescription().trim().isEmpty()) {
                                fullPrompt.append(": ").append(action.getDescription());
                            }
                            fullPrompt.append("\n");
                        }
                        fullPrompt.append("\n");
                        fullPrompt.append("CRITICAL ACTION DISPLAY RULES:\n");
                        fullPrompt.append("1. If the configuration rules mention displaying an action (e.g., 'display {action_Confirmation}'), ");
                        fullPrompt.append("you MUST include that exact action placeholder in your response text.\n");
                        fullPrompt.append("2. Use the EXACT placeholder format as shown in the rules (e.g., {action_Confirmation} or {action_order_selector}).\n");
                        fullPrompt.append("3. CRITICAL TIMING RULE: If the rules say 'WAIT FOR USER TO MESSAGE', 'once user provides', or 'after user provides', ");
                        fullPrompt.append("this means:\n");
                        fullPrompt.append("   - DO NOT display the action in the same response where you ask for information\n");
                        fullPrompt.append("   - ONLY display the action AFTER the user has provided the information in a SUBSEQUENT message\n");
                        fullPrompt.append("   - Check the conversation history above - if the user has ALREADY provided the information in a previous message, THEN display the action\n");
                        fullPrompt.append("   - If you are asking for information in your current response, DO NOT include any actions\n");
                        fullPrompt.append("4. STRICT ACTION TRIGGERING RULE - THIS IS CRITICAL:\n");
                        fullPrompt.append("   - Actions must ONLY be displayed when BOTH conditions are met:\n");
                        fullPrompt.append("     a) The configuration rules explicitly mention displaying that SPECIFIC action\n");
                        fullPrompt.append("     b) The user's current message ACTUALLY matches the condition in the rule\n");
                        fullPrompt.append("   - Example: If a rule says 'If a user asks about an order then display {action_order_selector}', ");
                        fullPrompt.append("you can ONLY display {action_order_selector} if the user ACTUALLY asked about an order.\n");
                        fullPrompt.append("   - If the user said 'My name is John', they did NOT ask about an order, so DO NOT display {action_order_selector}.\n");
                        fullPrompt.append("   - DO NOT display actions proactively, preemptively, or 'just in case'.\n");
                        fullPrompt.append("   - DO NOT display actions that are available but not relevant to the current conversation.\n");
                        fullPrompt.append("   - DO NOT add actions to responses unless the user's message explicitly triggers the rule condition.\n");
                        fullPrompt.append("5. If the rules mention an action but say to wait, check the conversation history first:\n");
                        fullPrompt.append("   - If the user has NOT yet provided the required information, ask for it WITHOUT displaying the action\n");
                        fullPrompt.append("   - If the user HAS provided the required information in a previous message, THEN display the action\n");
                        fullPrompt.append("6. FINAL CHECK: Before including any action placeholder, ask yourself:\n");
                        fullPrompt.append("   - Does the configuration rule explicitly say to display this action?\n");
                        fullPrompt.append("   - Does the user's current message match the condition in that rule?\n");
                        fullPrompt.append("   - If either answer is NO, DO NOT include the action.\n\n");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load chatbot actions: " + e.getMessage());
                }
            }
            
            // Add conversation history if available (only from current session)
            if (!isFirstMessage && conversationHistory != null && !conversationHistory.trim().isEmpty()) {
                fullPrompt.append("Previous conversation in this chat session:\n");
                fullPrompt.append(conversationHistory).append("\n\n");
                fullPrompt.append("IMPORTANT: Only reference the conversation above. Do not reference any other conversations or previous sessions.\n\n");
            }
            
            // Add current customer message
            fullPrompt.append("Current customer message (this is what the user just sent): ").append(customerMessage).append("\n\n");
            
            // Add explicit instruction about timing
            fullPrompt.append("TIMING INSTRUCTION: The 'Current customer message' above is what the user JUST sent. ");
            fullPrompt.append("The 'Previous conversation' above shows what happened BEFORE this message. ");
            fullPrompt.append("If the rules say to wait for the user to provide information, check if that information is in the 'Current customer message' or 'Previous conversation'. ");
            fullPrompt.append("If the information is in the 'Current customer message', you can display the action NOW. ");
            fullPrompt.append("If the information is NOT yet provided, ask for it WITHOUT displaying the action.\n\n");
            
            // RAG: Retrieve relevant articles from knowledge base (if needed)
            String knowledgeBaseContext = getKnowledgeBaseContext(customerMessage);
            if (knowledgeBaseContext != null && !knowledgeBaseContext.trim().isEmpty()) {
                fullPrompt.append("Knowledge base information:\n");
                fullPrompt.append(knowledgeBaseContext).append("\n\n");
            }
            
            // Extract formatting instructions for post-processing
            String formattingInstructions = extractFormattingInstructions(configurationContext);
            
            // Final instruction - minimal, let configuration drive behavior
            fullPrompt.append("Respond as a customer service agent following the configuration rules above.\n");
            
            // If this is an action-triggered message, suppress actions to prevent loops
            if (isActionTriggered) {
                fullPrompt.append("NOTE: This message was triggered by a user action. ");
                fullPrompt.append("Do NOT include any action placeholders in your response.\n");
            }
            
            fullPrompt.append("Your response:");
            
            // Log the full prompt for debugging
            System.out.println("=== FULL PROMPT FOR AI ===");
            System.out.println(fullPrompt.toString());
            System.out.println("=== END PROMPT ===\n");
            
            // Create request body for Ollama
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama3.2:3b");
            requestBody.put("prompt", fullPrompt.toString());
            requestBody.put("stream", false);
            // Lower temperature for more consistent rule following
            requestBody.put("options", Map.of(
                "temperature", 0.3,  // Lower temperature for stricter rule following
                "top_p", 0.9,
                "max_tokens", 300
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
            aiResponse = cleanAgentResponse(aiResponse);
            
            // Log action placeholders before cleaning
            Map<String, String> detectedActions = extractActionPlaceholders(aiResponse);
            if (!detectedActions.isEmpty()) {
                System.out.println("=== DETECTED ACTION PLACEHOLDERS ===");
                System.out.println("WARNING: Actions were detected in AI response, but they should only appear if explicitly mentioned in rules!");
                System.out.println("Customer message: " + customerMessage);
                System.out.println("Is action-triggered: " + isActionTriggered);
                for (Map.Entry<String, String> entry : detectedActions.entrySet()) {
                    System.out.println("Action: " + entry.getKey() + " (param: " + entry.getValue() + ")");
                }
                System.out.println("=== END ACTIONS ===");
            } else {
                System.out.println("=== NO ACTIONS DETECTED ===");
                System.out.println("AI response does not contain any action placeholders (this is correct if rules don't mention actions)");
            }
            
            // Remove any variable syntax that slipped through (but preserve action placeholders)
            // Action placeholders use {action_key} format (single braces), so we need to preserve them
            // First, temporarily replace action placeholders
            Map<String, String> actionPlaceholders = new HashMap<>();
            java.util.regex.Pattern actionPattern = java.util.regex.Pattern.compile("\\{action_([a-zA-Z0-9_]+)(?::([^}]+))?\\}");
            java.util.regex.Matcher actionMatcher = actionPattern.matcher(aiResponse);
            StringBuffer actionBuffer = new StringBuffer();
            int actionIndex = 0;
            while (actionMatcher.find()) {
                String placeholder = actionMatcher.group(0);
                String tempKey = "___ACTION_PLACEHOLDER_" + actionIndex + "___";
                actionPlaceholders.put(tempKey, placeholder);
                actionMatcher.appendReplacement(actionBuffer, tempKey);
                actionIndex++;
            }
            actionMatcher.appendTail(actionBuffer);
            String tempResponse = actionBuffer.toString();
            
            // Now remove variable syntax
            tempResponse = tempResponse.replaceAll("\\{\\{[^}]+\\}\\}", "").trim();
            tempResponse = tempResponse.replaceAll("\\[.*?PLACEHOLDER.*?\\]", "").trim();
            tempResponse = tempResponse.replaceAll("\\[ASK_USER_FOR_NAME\\]", "").trim();
            tempResponse = tempResponse.replaceAll("\\[CHECK_CONVERSATION_FOR_NAME_OR_ASK\\]", "").trim();
            
            // Restore action placeholders
            for (Map.Entry<String, String> entry : actionPlaceholders.entrySet()) {
                tempResponse = tempResponse.replace(entry.getKey(), entry.getValue());
            }
            aiResponse = tempResponse;
            
            // Remove meta-commentary about instructions/prompts
            aiResponse = aiResponse.replaceAll("(?i)(I notice|I see|the prompt|the instruction|the system|I will ignore|I'll ignore|to clarify|Here's my response:).*?\\n", "");
            aiResponse = aiResponse.replaceAll("(?i)However, I notice.*?\\.", "");
            
            // Clean up any double spaces or empty lines (but preserve newlines from formatting)
            // Only collapse multiple spaces, not newlines
            aiResponse = aiResponse.replaceAll("[ ]+", " ").trim();
            
            // CRITICAL: Remove actions if the AI is asking for information
            // This is a safety check to prevent actions from appearing when asking questions
            // BUT: Don't remove confirmation actions if user has already provided the information
            if (isAskingForInformation(aiResponse, customerMessage, conversationHistory)) {
                System.out.println("=== DETECTED: AI is asking for information - checking if actions should be removed ===");
                Map<String, String> actionsToRemove = extractActionPlaceholders(aiResponse);
                if (!actionsToRemove.isEmpty()) {
                    // Check if user has already provided information (e.g., name) and action is for confirmation
                    // In this case, the confirmation action IS the way to confirm, so we should keep it
                    List<String> actionsToKeep = new java.util.ArrayList<>();
                    for (String actionKey : actionsToRemove.keySet()) {
                        String lowerActionKey = actionKey.toLowerCase();
                        // If it's a confirmation action and user has provided their name, keep it
                        if (lowerActionKey.contains("confirm")) {
                            // Check if user message or history contains name-like information
                            String lowerCustomerMessage = customerMessage != null ? customerMessage.toLowerCase() : "";
                            String lowerHistory = conversationHistory != null ? conversationHistory.toLowerCase() : "";
                            boolean hasNameInfo = lowerCustomerMessage.contains("name is") || 
                                                 lowerCustomerMessage.contains("i'm") ||
                                                 lowerCustomerMessage.contains("i am") ||
                                                 lowerHistory.contains("name is") ||
                                                 lowerHistory.contains("i'm") ||
                                                 lowerHistory.contains("i am");
                            if (hasNameInfo) {
                                System.out.println("Keeping confirmation action - user has provided name information");
                                actionsToKeep.add(actionKey);
                            }
                        }
                    }
                    
                    // Remove actions that should be removed (exclude ones we're keeping)
                    List<String> finalActionsToRemove = new java.util.ArrayList<>();
                    for (String actionKey : actionsToRemove.keySet()) {
                        if (!actionsToKeep.contains(actionKey)) {
                            finalActionsToRemove.add(actionKey);
                        }
                    }
                    
                    if (!finalActionsToRemove.isEmpty()) {
                        System.out.println("Removing actions: " + finalActionsToRemove);
                        // Remove action placeholders from the response
                        for (String actionKey : finalActionsToRemove) {
                            aiResponse = aiResponse.replaceAll("\\{action_" + actionKey + "(?::[^}]+)?\\}", "").trim();
                        }
                        aiResponse = aiResponse.replaceAll("[ ]+", " ").trim();
                        System.out.println("Response after removing actions: " + aiResponse);
                    } else {
                        System.out.println("All actions are confirmation actions with user info provided - keeping them");
                    }
                }
            }
            
            // CRITICAL: Remove actions that don't match the user's message context
            // This prevents actions from appearing when they're not relevant
            Map<String, String> contextActions = extractActionPlaceholders(aiResponse);
            if (!contextActions.isEmpty() && customerMessage != null && !customerMessage.trim().isEmpty()) {
                String lowerCustomerMessage = customerMessage.toLowerCase();
                List<String> actionsToRemove = new java.util.ArrayList<>();
                
                for (String actionKey : contextActions.keySet()) {
                    // Check if action matches user's message context
                    boolean shouldKeep = isActionRelevantToMessage(actionKey, lowerCustomerMessage, conversationHistory);
                    if (!shouldKeep) {
                        actionsToRemove.add(actionKey);
                        System.out.println("=== REMOVING IRRELEVANT ACTION ===");
                        System.out.println("Action: " + actionKey);
                        System.out.println("User message: " + customerMessage);
                        System.out.println("Reason: Action does not match user's message context");
                    }
                }
                
                // Remove irrelevant actions
                if (!actionsToRemove.isEmpty()) {
                    for (String actionKey : actionsToRemove) {
                        aiResponse = aiResponse.replaceAll("\\{action_" + actionKey + "(?::[^}]+)?\\}", "").trim();
                    }
                    aiResponse = aiResponse.replaceAll("[ ]+", " ").trim();
                    System.out.println("Response after removing irrelevant actions: " + aiResponse);
                }
            }
            
            // Apply formatting rules post-processing to ensure they're followed
            // This MUST happen after cleaning to preserve the formatting
            if (formattingInstructions != null && !formattingInstructions.trim().isEmpty()) {
                System.out.println("Applying formatting rules to response...");
                aiResponse = applyFormattingRules(aiResponse, formattingInstructions);
                System.out.println("Formatted response: " + aiResponse.replace("\n", "\\n"));
            }
            
            // Final check: Log if action placeholders are still present
            Map<String, String> finalActions = extractActionPlaceholders(aiResponse);
            if (!finalActions.isEmpty()) {
                System.out.println("=== FINAL ACTION CHECK ===");
                System.out.println("Actions still in response: " + finalActions.keySet());
                System.out.println("Response text: " + aiResponse);
                System.out.println("These actions will be sent to frontend for rendering");
            } else {
                System.out.println("=== FINAL ACTION CHECK ===");
                System.out.println("No action placeholders found in final response");
                System.out.println("Response text: " + aiResponse);
                System.out.println("If configuration mentioned an action, the AI may not have included it");
            }
            
            System.out.println("Generated AI agent response for chatbot: " + aiResponse);
            return aiResponse;
            
        } catch (Exception e) {
            System.err.println("=== ERROR GENERATING CHATBOT RESPONSE ===");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Error class: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            return "Thank you for contacting us. I'm here to help you with your inquiry. Could you please provide more details so I can assist you better?";
        }
    }
    
    /**
     * Check if a message is a simple conversational message that doesn't need RAG
     */
    private boolean isSimpleConversationalMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return true;
        }
        
        String lowerMessage = message.toLowerCase().trim();
        
        // List of simple greetings and conversational phrases that don't need RAG
        String[] conversationalPatterns = {
            "hey", "hi", "hello", "greetings", "good morning", "good afternoon", "good evening",
            "how are you", "how's it going", "what's up", "thanks", "thank you", "bye", "goodbye",
            "see you", "talk later", "ok", "okay", "sure", "yes", "no", "maybe", "alright",
            "got it", "understood", "i see", "cool", "nice", "great", "awesome", "perfect"
        };
        
        // Check if message is just a greeting or very short conversational phrase
        for (String pattern : conversationalPatterns) {
            if (lowerMessage.equals(pattern) || lowerMessage.startsWith(pattern + " ") || lowerMessage.endsWith(" " + pattern)) {
                // If it's just the pattern or pattern + punctuation, skip RAG
                String withoutPunctuation = lowerMessage.replaceAll("[^a-z\\s]", "").trim();
                if (withoutPunctuation.equals(pattern) || withoutPunctuation.length() < 20) {
                    System.out.println("RAG: Detected simple conversational message, skipping knowledge base search");
                    return true;
                }
            }
        }
        
        // If message is very short (less than 10 characters), likely conversational
        if (lowerMessage.replaceAll("[^a-z]", "").length() < 10) {
            System.out.println("RAG: Message too short, likely conversational, skipping knowledge base search");
            return true;
        }
        
        // Check if message contains question words or seems to be asking for information
        String[] questionIndicators = {"what", "when", "where", "who", "why", "how", "which", "can you", "could you", "tell me", "explain", "help", "information", "about", "wondering", "wonder", "question"};
        boolean hasQuestionIndicator = false;
        for (String indicator : questionIndicators) {
            if (lowerMessage.contains(indicator)) {
                hasQuestionIndicator = true;
                System.out.println("RAG: Detected question indicator: '" + indicator + "' in message: '" + message + "'");
                break;
            }
        }
        
        // If message has question indicators, it's definitely not conversational - allow RAG
        if (hasQuestionIndicator) {
            System.out.println("RAG: Message contains question indicators - allowing knowledge base search");
            return false; // NOT conversational, allow RAG
        }
        
        // If no question indicators and message is short, likely conversational
        if (lowerMessage.length() < 30) {
            System.out.println("RAG: No question indicators and message is short (" + lowerMessage.length() + " chars), likely conversational, skipping knowledge base search");
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if the AI response is asking for information from the user
     * This is used to prevent actions from appearing when the AI is asking questions
     */
    private boolean isAskingForInformation(String aiResponse, String customerMessage, String conversationHistory) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return false;
        }
        
        String lowerResponse = aiResponse.toLowerCase();
        
        // Patterns that indicate the AI is asking for information
        String[] askingPatterns = {
            "could you please",
            "can you please",
            "would you please",
            "please tell me",
            "please provide",
            "please share",
            "please let me know",
            "could you tell me",
            "can you tell me",
            "would you tell me",
            "tell me your",
            "what is your",
            "what's your",
            "may i have",
            "i need to know",
            "i'd like to know",
            "i would like to know",
            "could you",
            "can you",
            "would you"
        };
        
        // Check if response contains asking patterns
        boolean containsAskingPattern = false;
        for (String pattern : askingPatterns) {
            if (lowerResponse.contains(pattern)) {
                containsAskingPattern = true;
                break;
            }
        }
        
        // Also check if the response ends with a question mark (strong indicator)
        boolean endsWithQuestion = lowerResponse.trim().endsWith("?");
        
        // Check if the customer message doesn't contain the information being asked for
        // (e.g., if AI asks for name, check if name is in customer message or history)
        boolean informationNotProvided = true;
        if (customerMessage != null && !customerMessage.trim().isEmpty()) {
            String lowerCustomerMessage = customerMessage.toLowerCase();
            // If customer message is very short or is a greeting, they likely haven't provided info yet
            if (lowerCustomerMessage.length() < 20 || 
                lowerCustomerMessage.matches("^(hi|hello|hey|greetings|good morning|good afternoon|good evening).*")) {
                informationNotProvided = true;
            } else {
                // Check if response asks for name and customer message might contain a name
                if (lowerResponse.contains("name") && lowerCustomerMessage.length() > 5) {
                    // Check if customer message actually contains name information
                    boolean hasNameInfo = lowerCustomerMessage.contains("name is") || 
                                         lowerCustomerMessage.contains("i'm") ||
                                         lowerCustomerMessage.contains("i am") ||
                                         lowerCustomerMessage.contains("my name");
                    if (hasNameInfo) {
                        informationNotProvided = false; // User has provided name
                    } else {
                        informationNotProvided = true;
                    }
                } else if (lowerResponse.contains("confirm")) {
                    // If AI is asking to confirm something, check if user has provided that information
                    // Check conversation history too
                    String lowerHistory = conversationHistory != null ? conversationHistory.toLowerCase() : "";
                    boolean hasInfoToConfirm = lowerCustomerMessage.contains("name is") || 
                                              lowerCustomerMessage.contains("i'm") ||
                                              lowerCustomerMessage.contains("i am") ||
                                              lowerCustomerMessage.contains("my name") ||
                                              lowerHistory.contains("name is") ||
                                              lowerHistory.contains("i'm") ||
                                              lowerHistory.contains("i am");
                    if (hasInfoToConfirm) {
                        informationNotProvided = false; // User has provided info to confirm
                    } else {
                        informationNotProvided = true;
                    }
                } else {
                    informationNotProvided = true;
                }
            }
        }
        
        // If the AI is asking a question AND the information hasn't been provided, it's asking for information
        boolean isAsking = (containsAskingPattern || endsWithQuestion) && informationNotProvided;
        
        if (isAsking) {
            System.out.println("=== DETECTED: AI is asking for information ===");
            System.out.println("Response: " + aiResponse.substring(0, Math.min(100, aiResponse.length())));
            System.out.println("Contains asking pattern: " + containsAskingPattern);
            System.out.println("Ends with question: " + endsWithQuestion);
            System.out.println("Information not provided: " + informationNotProvided);
        }
        
        return isAsking;
    }
    
    /**
     * Check if an action is relevant to the user's message
     * This prevents actions from appearing when they don't match the conversation context
     */
    private boolean isActionRelevantToMessage(String actionKey, String lowerCustomerMessage, String conversationHistory) {
        if (actionKey == null || lowerCustomerMessage == null) {
            return true; // Default to keeping if we can't determine
        }
        
        String lowerActionKey = actionKey.toLowerCase();
        
        // Define relevance rules for specific actions
        // If action is order_selector, user message should mention order-related terms
        if (lowerActionKey.contains("order")) {
            String[] orderKeywords = {"order", "orders", "purchase", "bought", "buy", "transaction", "delivery", "shipping", "track"};
            boolean hasOrderKeyword = false;
            for (String keyword : orderKeywords) {
                if (lowerCustomerMessage.contains(keyword)) {
                    hasOrderKeyword = true;
                    break;
                }
            }
            if (!hasOrderKeyword && conversationHistory != null) {
                // Check conversation history too
                String lowerHistory = conversationHistory.toLowerCase();
                for (String keyword : orderKeywords) {
                    if (lowerHistory.contains(keyword)) {
                        hasOrderKeyword = true;
                        break;
                    }
                }
            }
            if (!hasOrderKeyword) {
                System.out.println("Action 'order_selector' is not relevant - user message doesn't mention orders");
                return false;
            }
        }
        
        // For confirmation action, it's usually relevant when user provides information
        // (name, email, etc.) - this is handled by the timing rules, so we allow it here
        if (lowerActionKey.contains("confirm")) {
            // Confirmation actions are typically triggered by configuration rules
            // after user provides information, so we allow them
            return true;
        }
        
        // For other actions, be permissive but log for debugging
        System.out.println("Action '" + actionKey + "' relevance check: allowing (no specific rule)");
        return true;
    }
    
    /**
     * Get relevant knowledge base articles for RAG (Retrieval Augmented Generation)
     * Uses semantic search with embeddings if available, falls back to keyword search
     */
    private String getKnowledgeBaseContext(String customerMessage) {
        try {
            if (customerMessage == null || customerMessage.trim().isEmpty()) {
                System.out.println("RAG: Customer message is empty, skipping knowledge base search");
                return null;
            }
            
            // Skip RAG for simple conversational messages
            if (isSimpleConversationalMessage(customerMessage)) {
                return null;
            }
            
            System.out.println("RAG: Searching knowledge base for: '" + customerMessage + "'");
            
            // Try semantic search first (if RAG pipeline is available)
            if (ragPipeline != null) {
                try {
                    // Retrieve chunks with stricter similarity threshold (now 0.65)
                    String semanticContext = ragPipeline.retrieveContext(customerMessage, 5);
                    if (semanticContext != null && !semanticContext.trim().isEmpty()) {
                        System.out.println("RAG: Using semantic search (embeddings) - found relevant articles");
                        System.out.println("RAG: Generated context (length: " + semanticContext.length() + " chars)");
                        if (semanticContext.length() < 200) {
                            System.out.println("RAG: WARNING - Context is very short (" + semanticContext.length() + " chars). This might indicate indexing issues.");
                        }
                        return semanticContext;
                    } else {
                        System.out.println("RAG: Semantic search found no highly relevant articles (similarity < 0.65) - skipping RAG to avoid irrelevant context");
                        System.out.println("RAG: This is normal if the Knowledge Base doesn't contain information relevant to the query");
                        return null; // Don't fall back to keyword search if semantic search found nothing relevant
                    }
                } catch (Exception e) {
                    System.err.println("RAG: Semantic search failed, falling back to keyword search: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Fallback to keyword-based search
            System.out.println("RAG: Using keyword-based search (fallback)");
            List<com.prototype.entity.Article> relevantArticles = knowledgeBaseService.searchArticles(customerMessage);
            
            System.out.println("RAG: Found " + relevantArticles.size() + " relevant articles");
            
            if (relevantArticles.isEmpty()) {
                System.out.println("RAG: No relevant articles found");
                return null;
            }
            
            // Limit to top 3 most relevant articles to avoid token limits
            int maxArticles = Math.min(3, relevantArticles.size());
            StringBuilder context = new StringBuilder();
            
            for (int i = 0; i < maxArticles; i++) {
                com.prototype.entity.Article article = relevantArticles.get(i);
                context.append("Article: ").append(article.getTitle()).append("\n");
                if (article.getCategory() != null) {
                    context.append("Category: ").append(article.getCategory().getName()).append("\n");
                }
                // Strip HTML tags for cleaner context
                String cleanContent = article.getContent().replaceAll("<[^>]*>", " ");
                cleanContent = cleanContent.replaceAll("\\s+", " ").trim();
                // Increase content length limit for better context
                if (cleanContent.length() > 800) {
                    cleanContent = cleanContent.substring(0, 800) + "...";
                }
                context.append("Content: ").append(cleanContent).append("\n\n");
            }
            
            String contextString = context.toString();
            System.out.println("RAG: Generated context (length: " + contextString.length() + " chars)");
            
            return contextString;
        } catch (Exception e) {
            System.err.println("Failed to retrieve knowledge base context: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generate AI agent response for simulation - acts as a customer service agent
     */
    public String generateAgentResponseForSimulation(String customerMessage, String context, Long ticketId) {
        try {
            // Get conversation history if ticket ID is provided
            String conversationHistory = "";
            String ticketContext = "";
            
            if (ticketId != null) {
                Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
                if (ticketOpt.isPresent()) {
                    Ticket ticket = ticketOpt.get();
                    
                    // Build ticket context
                    ticketContext = String.format("Ticket #%d: %s - %s (Status: %s, Priority: %s, Category: %s)", 
                        ticket.getId(), ticket.getSubject(), ticket.getDescription(),
                        ticket.getStatus(), ticket.getPriority(), 
                        ticket.getCategory() != null ? ticket.getCategory().toString() : "N/A");
                    
                    // Get conversation history (last 10 messages for context)
                    List<TicketMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
                    if (!messages.isEmpty()) {
                        conversationHistory = buildConversationHistoryForAgent(messages);
                    }
                }
            }
            
                   // Get AI configurations - try new rule-based system first, fall back to old system
                   String configurationContext = "";
                   Ticket ticketForContext = null;
                   try {
                       if (ticketId != null) {
                           Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
                           if (ticketOpt.isPresent()) {
                               ticketForContext = ticketOpt.get();
                           }
                       }
                       
                       if (airuleService != null) {
                           configurationContext = airuleService.getAllRulesAsPrompt(ticketForContext);
                       } else {
                           // Fallback to old system
                           if (ticketForContext != null) {
                               configurationContext = configurationService.getRelevantConfigurationsAsPrompt(ticketForContext);
                           } else {
                               configurationContext = configurationService.getAllActiveConfigurationsAsPrompt();
                           }
                       }
                   } catch (Exception e) {
                       System.err.println("Failed to load AI configurations: " + e.getMessage());
                       // Fallback to old system
                       try {
                           if (ticketForContext != null) {
                               configurationContext = configurationService.getRelevantConfigurationsAsPrompt(ticketForContext);
                           } else {
                               configurationContext = configurationService.getAllActiveConfigurationsAsPrompt();
                           }
                       } catch (Exception e2) {
                           System.err.println("Failed to load AI configurations (fallback): " + e2.getMessage());
                       }
                   }
            
            // Create the base prompt for a customer service agent
            String baseSystemPrompt = "You are a professional customer service agent responding to a customer inquiry. " +
                "Your goal is to provide helpful, accurate, and empathetic support. " +
                "Be professional, clear, and solution-oriented. " +
                "Acknowledge the customer's concern, provide relevant information, and offer next steps when appropriate. " +
                "Keep responses concise but complete - typically 2-4 sentences. " +
                "Use a friendly but professional tone. " +
                "Don't put your response in quotes - just respond directly as the agent.";
            
            StringBuilder fullPrompt = new StringBuilder();
            
            // RAG: Retrieve relevant articles from knowledge base FIRST (most important - facts)
            System.out.println("=== RAG DEBUG [SIMULATION]: Starting knowledge base search ===");
            System.out.println("Customer message: '" + customerMessage + "'");
            String knowledgeBaseContext = getKnowledgeBaseContext(customerMessage);
            if (knowledgeBaseContext != null && !knowledgeBaseContext.trim().isEmpty()) {
                System.out.println("=== RAG DEBUG [SIMULATION]: Adding knowledge base context to prompt ===");
                // Put RAG context FIRST and make it very prominent
                fullPrompt.append("=== CRITICAL: KNOWLEDGE BASE INFORMATION ===\n");
                fullPrompt.append("IMPORTANT: The following information from the knowledge base directly answers the customer's question. ");
                fullPrompt.append("YOU MUST USE THIS INFORMATION to answer. Do not ask for information that is already provided here.\n\n");
                fullPrompt.append(knowledgeBaseContext).append("\n\n");
                fullPrompt.append("=== END KNOWLEDGE BASE ===\n\n");
                fullPrompt.append("INSTRUCTION: If the knowledge base above contains information that answers the customer's question, ");
                fullPrompt.append("use that information directly in your response. Do not ask the customer for information that is already in the knowledge base.\n\n");
            } else {
                System.out.println("=== RAG DEBUG [SIMULATION]: No knowledge base context found or empty ===");
            }
            
            // Add comprehensive instruction system for AI customization
            fullPrompt.append("=== AI CUSTOMIZATION INSTRUCTION SYSTEM ===\n");
            fullPrompt.append("You are a highly configurable AI agent. The following instructions explain how to interpret and execute ");
            fullPrompt.append("configuration rules provided by the platform administrator.\n\n");
            
            fullPrompt.append("1. TEMPLATE VARIABLES:\n");
            fullPrompt.append("   - Variables like {{user.name}}, {{user.email}}, {{order.total}}, etc. refer to information from the ticket/user context.\n");
            fullPrompt.append("   - These variables are automatically replaced with actual values when available.\n");
            fullPrompt.append("   - If a configuration says 'if {{variable}} is empty, ask for it', check if the variable has a value.\n");
            fullPrompt.append("   - If empty, follow the instruction to ask for it.\n");
            fullPrompt.append("   - Once you have the information, use it throughout the conversation.\n\n");
            
            fullPrompt.append("2. RESPONSE FORMATTING:\n");
            fullPrompt.append("   - Follow ALL formatting instructions exactly as specified in the configuration.\n");
            fullPrompt.append("   - If instructed to use line breaks, new lines, or separate messages, do so precisely.\n");
            fullPrompt.append("   - If instructed to respond in multiple messages, structure your response accordingly.\n");
            fullPrompt.append("   - Formatting instructions are MANDATORY and must be followed.\n\n");
            
            fullPrompt.append("3. CONFIGURATION EXECUTION:\n");
            fullPrompt.append("   - All instructions in the AI Configuration Guidelines section below are actionable rules.\n");
            fullPrompt.append("   - Execute them as specified - they are not suggestions, they are requirements.\n");
            fullPrompt.append("   - If a rule conflicts with general behavior, the rule takes precedence.\n");
            fullPrompt.append("   - Complex behaviors (multiple messages, conditional logic, etc.) should be followed exactly.\n\n");
            
            fullPrompt.append("=== END INSTRUCTION SYSTEM ===\n\n");
            
            // Add configuration context AFTER RAG (guidelines, not facts)
            if (configurationContext != null && !configurationContext.trim().isEmpty()) {
                fullPrompt.append("=== AI CONFIGURATION GUIDELINES (EXECUTE THESE RULES) ===\n");
                fullPrompt.append("The following are configuration rules set by the platform administrator. ");
                fullPrompt.append("These are NOT suggestions - they are REQUIREMENTS that you must follow.\n\n");
                fullPrompt.append("Execute these rules exactly as specified:\n\n");
                fullPrompt.append(configurationContext).append("\n\n");
                fullPrompt.append("=== END CONFIGURATION GUIDELINES ===\n\n");
            }
            
            fullPrompt.append("=== BASE SYSTEM PROMPT ===\n");
            fullPrompt.append(baseSystemPrompt).append("\n\n");
            
            if (!ticketContext.isEmpty()) {
                fullPrompt.append("TICKET CONTEXT:\n").append(ticketContext).append("\n\n");
            }
            
            // Extract and emphasize formatting instructions from configuration
            String formattingInstructions = extractFormattingInstructions(configurationContext);
            if (formattingInstructions != null && !formattingInstructions.trim().isEmpty()) {
                fullPrompt.append("=== RESPONSE FORMATTING REQUIREMENTS (CRITICAL - MUST FOLLOW) ===\n");
                fullPrompt.append("These formatting instructions are MANDATORY and must be strictly followed:\n\n");
                fullPrompt.append(formattingInstructions).append("\n\n");
                fullPrompt.append("=== END FORMATTING REQUIREMENTS ===\n\n");
            }
            
            if (!conversationHistory.isEmpty()) {
                fullPrompt.append("=== PREVIOUS CONVERSATION CONTEXT (FOR REFERENCE ONLY) ===\n");
                fullPrompt.append("The following is previous conversation for context. ");
                fullPrompt.append("IMPORTANT: Focus on the CURRENT MESSAGE below, not the old messages above.\n");
                fullPrompt.append(conversationHistory).append("\n\n");
                fullPrompt.append("=== END PREVIOUS CONTEXT ===\n\n");
            }
            
            fullPrompt.append("=== CURRENT CUSTOMER MESSAGE (RESPOND TO THIS) ===\n");
            fullPrompt.append("CRITICAL: The customer's current question is below. ");
            fullPrompt.append("You MUST respond to THIS message, not previous messages in the conversation history.\n");
            fullPrompt.append(customerMessage).append("\n\n");
            fullPrompt.append("=== END CURRENT MESSAGE ===\n\n");
            
            // Reinforce formatting before response
            if (formattingInstructions != null && !formattingInstructions.trim().isEmpty()) {
                fullPrompt.append("CRITICAL FORMATTING REMINDER: You MUST follow the formatting requirements specified above. ");
                fullPrompt.append("If the requirements specify line breaks, new lines, or sentence formatting, you MUST apply them exactly as specified. ");
                fullPrompt.append("Do not ignore formatting instructions.\n\n");
            }
            fullPrompt.append("Your response as the customer service agent (apply all formatting requirements):");
            
            // Create request body for Ollama
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama3.2:3b");
            requestBody.put("prompt", fullPrompt.toString());
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", 0.5,  // Lower temperature for more consistent, professional responses
                "top_p", 0.8,
                "max_tokens", 300    // Increased to allow formatted responses with line breaks
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
            
            // Clean up the response first
            aiResponse = cleanAgentResponse(aiResponse);
            
            // Apply formatting rules post-processing to ensure they're followed
            // This MUST happen after cleaning to preserve the formatting
            if (formattingInstructions != null && !formattingInstructions.trim().isEmpty()) {
                System.out.println("Applying formatting rules to simulation response...");
                aiResponse = applyFormattingRules(aiResponse, formattingInstructions);
                System.out.println("Formatted simulation response: " + aiResponse.replace("\n", "\\n"));
            }
            
            System.out.println("Generated AI agent response for simulation: " + aiResponse);
            return aiResponse;
            
        } catch (Exception e) {
            System.err.println("Failed to generate AI agent response: " + e.getMessage());
            e.printStackTrace();
            // Fallback response
            return "Thank you for contacting us. I'm here to help you with your inquiry. Could you please provide more details so I can assist you better?";
        }
    }
    
    /**
     * Build conversation history string from messages (for agent perspective)
     */
    private String buildConversationHistoryForAgent(List<TicketMessage> messages) {
        // Get last 8 messages to keep context manageable but comprehensive
        List<TicketMessage> recentMessages = messages.size() > 8 ? 
            messages.subList(messages.size() - 8, messages.size()) : messages;
            
        return recentMessages.stream()
            .map(msg -> {
                String sender;
                if (msg.getSenderType().toString().equals("USER")) {
                    sender = "Customer";
                } else if (msg.getSenderType().toString().equals("AGENT")) {
                    sender = "Agent";
                } else {
                    sender = "System";
                }
                return String.format("%s: %s", sender, msg.getMessage());
            })
            .collect(Collectors.joining("\n"));
    }
    
    /**
     * Clean up agent response
     */
    /**
     * Extract action placeholders from AI response
     * Returns a map of action keys found in the response
     */
    public Map<String, String> extractActionPlaceholders(String response) {
        Map<String, String> actions = new HashMap<>();
        // Pattern: {action_key} or {action_key:param}
        // Case-insensitive matching to handle variations like {action_Confirmation} vs {action_confirmation}
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{action_([a-zA-Z0-9_]+)(?::([^}]+))?\\}", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(response);
        
        while (matcher.find()) {
            String actionKey = matcher.group(1);
            String param = matcher.group(2);
            // Normalize to lowercase for consistency (action keys are case-sensitive in database)
            // But we'll try both the original case and lowercase when looking up
            actions.put(actionKey, param != null ? param : "");
            System.out.println("Extracted action placeholder: " + actionKey + " (param: " + (param != null ? param : "none") + ")");
        }
        
        return actions;
    }
    
    private String cleanAgentResponse(String response) {
        // Remove any unwanted prefixes or artifacts
        response = response.trim();
        
        // Remove common AI response prefixes for agent responses
        if (response.startsWith("Agent:")) {
            response = response.substring("Agent:".length()).trim();
        }
        if (response.startsWith("Response:")) {
            response = response.substring("Response:".length()).trim();
        }
        if (response.startsWith("Customer service agent:")) {
            response = response.substring("Customer service agent:".length()).trim();
        }
        if (response.startsWith("Support agent:")) {
            response = response.substring("Support agent:".length()).trim();
        }
        
        // Remove surrounding quotes if they wrap the entire response
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
     * Extract formatting-related instructions from AI configuration
     * Looks for rules that mention formatting, line breaks, messages, indentation, etc.
     * Returns all rules that contain formatting-related keywords
     */
    private String extractFormattingInstructions(String configurationContext) {
        if (configurationContext == null || configurationContext.trim().isEmpty()) {
            return null;
        }
        
        StringBuilder formattingRules = new StringBuilder();
        String[] lines = configurationContext.split("\n");
        boolean inFormattingRule = false;
        String currentRuleTitle = null;
        StringBuilder currentRuleContent = new StringBuilder();
        
        // Expanded keywords that indicate formatting instructions
        String[] formattingKeywords = {
            "format", "formatting", "line break", "new line", "sentence", "sentences",
            "indent", "indentation", "message", "messages", "multiple", "bullet", "list",
            "paragraph", "structure", "layout", "style", "presentation", "short", "clear",
            "separate", "each", "on a new", "per line", "line by line", "break", "lines"
        };
        
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            boolean isFormattingRelated = false;
            
            // Check if line contains formatting keywords
            for (String keyword : formattingKeywords) {
                if (lowerLine.contains(keyword)) {
                    isFormattingRelated = true;
                    break;
                }
            }
            
            // Check if this is a rule title (starts with [)
            if (line.trim().startsWith("[") && line.trim().endsWith("]")) {
                // Save previous rule if it was formatting-related
                if (inFormattingRule && currentRuleTitle != null && currentRuleContent.length() > 0) {
                    formattingRules.append(currentRuleTitle).append("\n");
                    formattingRules.append(currentRuleContent.toString().trim()).append("\n\n");
                }
                
                // Start new rule
                currentRuleTitle = line;
                currentRuleContent = new StringBuilder();
                inFormattingRule = isFormattingRelated;
            } else if (inFormattingRule) {
                // Continue adding lines until we hit another rule or section
                if (line.trim().startsWith("===")) {
                    // End of section, save current rule
                    if (currentRuleTitle != null && currentRuleContent.length() > 0) {
                        formattingRules.append(currentRuleTitle).append("\n");
                        formattingRules.append(currentRuleContent.toString().trim()).append("\n\n");
                    }
                    inFormattingRule = false;
                    currentRuleTitle = null;
                    currentRuleContent = new StringBuilder();
                } else {
                    currentRuleContent.append(line).append("\n");
                }
            }
        }
        
        // Save last rule if formatting-related
        if (inFormattingRule && currentRuleTitle != null && currentRuleContent.length() > 0) {
            formattingRules.append(currentRuleTitle).append("\n");
            formattingRules.append(currentRuleContent.toString().trim()).append("\n\n");
        }
        
        String result = formattingRules.toString().trim();
        return result.isEmpty() ? null : result;
    }
    
    /**
     * Post-process AI response to apply formatting rules if needed
     * This ensures formatting instructions are actually applied even if the AI model doesn't follow them perfectly
     */
    private String applyFormattingRules(String response, String formattingInstructions) {
        if (response == null || response.trim().isEmpty() || formattingInstructions == null || formattingInstructions.trim().isEmpty()) {
            return response;
        }
        
        String lowerFormatting = formattingInstructions.toLowerCase();
        
        // Check if formatting requires each sentence on a new line
        boolean requiresNewLinePerSentence = (lowerFormatting.contains("each sentence") || lowerFormatting.contains("sentence")) && 
                                            (lowerFormatting.contains("new line") || lowerFormatting.contains("separate line") || 
                                             lowerFormatting.contains("on a new") || lowerFormatting.contains("per line"));
        
        if (requiresNewLinePerSentence) {
            System.out.println("Formatting rule detected: Each sentence on a new line");
            System.out.println("Original response: " + response);
            
            // Split response into sentences and put each on a new line
            // Improved sentence detection: split on periods, exclamation marks, question marks
            // Handle both with and without spaces after punctuation
            String[] sentences = response.split("(?<=[.!?])(?=\\s+|$)");
            
            System.out.println("Detected " + sentences.length + " sentences");
            
            if (sentences.length > 1) {
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < sentences.length; i++) {
                    String sentence = sentences[i].trim();
                    if (!sentence.isEmpty()) {
                        if (i > 0) {
                            formatted.append("\n");
                        }
                        formatted.append(sentence);
                        // Ensure sentence ends with punctuation if it doesn't already
                        if (!sentence.matches(".*[.!?]$")) {
                            formatted.append(".");
                        }
                    }
                }
                String result = formatted.toString();
                System.out.println("Formatted response (with \\n): " + result.replace("\n", "\\n"));
                return result;
            } else {
                // Even if only one sentence detected, if the rule says "each sentence on new line",
                // we might need to split by other patterns (e.g., "and", "but", etc.)
                // For now, just return as-is if only one sentence
                System.out.println("Only one sentence detected, returning as-is");
            }
        }
        
        return response;
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