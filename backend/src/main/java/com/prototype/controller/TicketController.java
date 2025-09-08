package com.prototype.controller;

import com.prototype.entity.Ticket;
import com.prototype.entity.TicketMessage;
import com.prototype.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tickets")
@CrossOrigin(origins = "*")
public class TicketController {
    
    @Autowired
    private TicketService ticketService;
    
    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        List<Ticket> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        Optional<Ticket> ticket = ticketService.getTicketById(id);
        return ticket.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<TicketMessage>> getTicketMessages(@PathVariable Long id) {
        List<TicketMessage> messages = ticketService.getTicketMessages(id);
        return ResponseEntity.ok(messages);
    }
    
    @PostMapping("/{id}/messages")
    public ResponseEntity<TicketMessage> addMessage(@PathVariable Long id, @RequestBody TicketMessage message) {
        Optional<Ticket> ticket = ticketService.getTicketById(id);
        if (ticket.isPresent()) {
            message.setTicket(ticket.get());
            TicketMessage savedMessage = ticketService.addMessage(message);
            return ResponseEntity.ok(savedMessage);
        }
        return ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id, @RequestBody Ticket ticketUpdate) {
        Optional<Ticket> existingTicket = ticketService.getTicketById(id);
        if (existingTicket.isPresent()) {
            Ticket ticket = existingTicket.get();
            ticket.setStatus(ticketUpdate.getStatus());
            ticket.setPriority(ticketUpdate.getPriority());
            Ticket updatedTicket = ticketService.updateTicket(ticket);
            return ResponseEntity.ok(updatedTicket);
        }
        return ResponseEntity.notFound().build();
    }
}
