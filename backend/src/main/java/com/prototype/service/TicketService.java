package com.prototype.service;

import com.prototype.entity.Ticket;
import com.prototype.entity.TicketMessage;
import com.prototype.entity.TicketStatus;
import com.prototype.entity.TicketPriority;
import com.prototype.entity.TicketCategory;
import com.prototype.entity.SenderType;
import com.prototype.entity.User;
import com.prototype.repository.TicketRepository;
import com.prototype.repository.TicketMessageRepository;
import com.prototype.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TicketService {
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TicketMessageRepository messageRepository;
    
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAllWithUser();
    }
    
    public Optional<Ticket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }
    
    public List<Ticket> getTicketsByUser(String userUid) {
        return ticketRepository.findByUserUid(userUid);
    }
    
    public List<TicketMessage> getTicketMessages(Long ticketId) {
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }
    
    public Ticket createTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }
    
    public Ticket updateTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }
    
    public TicketMessage addMessage(TicketMessage message) {
        // Generate ID for new messages (since we removed @GeneratedValue)
        if (message.getId() == null) {
            Long maxId = messageRepository.findAll().stream()
                .mapToLong(TicketMessage::getId)
                .max()
                .orElse(0L);
            message.setId(maxId + 1);
        }
        return messageRepository.save(message);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(String uid) {
        return userRepository.findById(uid);
    }
    
    /**
     * Create a ticket from chatbot conversation
     */
    public Ticket createTicketFromChat(String subject, String description, List<ChatMessage> messages) {
        try {
            // Create a default user for chatbot tickets (or use first available user)
            User chatbotUser = userRepository.findById("chatbot-user")
                .orElseGet(() -> {
                    // Check if any users exist, if so use the first one
                    List<User> existingUsers = userRepository.findAll();
                    if (!existingUsers.isEmpty()) {
                        return existingUsers.get(0);
                    }
                    // Create a default user if none exists
                    User defaultUser = new User();
                    defaultUser.setUid("chatbot-user");
                    defaultUser.setName("Chatbot User");
                    defaultUser.setEmail("chatbot@example.com");
                    defaultUser.setLocation("Unknown");
                    defaultUser.setDevice("Web");
                    defaultUser.setPhoneNumber("N/A");
                    return userRepository.save(defaultUser);
                });
            
            // Generate ticket ID
            Long ticketId = ticketRepository.findAll().stream()
                .mapToLong(Ticket::getId)
                .max()
                .orElse(0L);
            ticketId++;
            
            System.out.println("Creating ticket with ID: " + ticketId);
            
            // Create ticket
            Ticket ticket = new Ticket();
            ticket.setId(ticketId);
            ticket.setSubject(subject != null && !subject.trim().isEmpty() ? subject : "Chatbot Conversation");
            ticket.setDescription(description != null && !description.trim().isEmpty() ? description : "Conversation started via chatbot");
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setPriority(TicketPriority.MEDIUM);
            ticket.setCategory(TicketCategory.GENERAL);
            ticket.setUser(chatbotUser);
            ticket = ticketRepository.save(ticket);
            
            System.out.println("Ticket saved with ID: " + ticket.getId());
            
            // Add messages
            if (messages != null && !messages.isEmpty()) {
                Long messageId = messageRepository.findAll().stream()
                    .mapToLong(TicketMessage::getId)
                    .max()
                    .orElse(0L);
                
                System.out.println("Adding " + messages.size() + " messages to ticket");
                
                for (ChatMessage chatMsg : messages) {
                    try {
                        messageId++;
                        TicketMessage ticketMessage = new TicketMessage();
                        ticketMessage.setId(messageId);
                        ticketMessage.setMessage(chatMsg.getMessage());
                        ticketMessage.setSenderType(SenderType.valueOf(chatMsg.getSenderType()));
                        ticketMessage.setSenderName(chatMsg.getSenderName() != null ? chatMsg.getSenderName() : 
                            (chatMsg.getSenderType().equals("USER") ? "User" : "AI Agent"));
                        ticketMessage.setTicket(ticket);
                        ticketMessage.setCreatedAt(LocalDateTime.now());
                        messageRepository.save(ticketMessage);
                    } catch (Exception e) {
                        System.err.println("Error saving message: " + e.getMessage());
                        e.printStackTrace();
                        // Continue with next message
                    }
                }
                
                System.out.println("All messages saved successfully");
            }
            
            return ticket;
        } catch (Exception e) {
            System.err.println("Error in createTicketFromChat: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    // Inner class for chat message
    public static class ChatMessage {
        private String message;
        private String senderType;
        private String senderName;
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getSenderType() { return senderType; }
        public void setSenderType(String senderType) { this.senderType = senderType; }
        
        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }
    }
}
