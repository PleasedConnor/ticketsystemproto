package com.prototype.service;

import com.prototype.entity.Ticket;
import com.prototype.entity.TicketMessage;
import com.prototype.entity.User;
import com.prototype.repository.TicketRepository;
import com.prototype.repository.TicketMessageRepository;
import com.prototype.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
