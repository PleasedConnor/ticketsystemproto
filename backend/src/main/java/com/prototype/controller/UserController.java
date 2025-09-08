package com.prototype.controller;

import com.prototype.entity.Ticket;
import com.prototype.entity.User;
import com.prototype.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private TicketService ticketService;
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = ticketService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{uid}")
    public ResponseEntity<User> getUserById(@PathVariable String uid) {
        Optional<User> user = ticketService.getUserById(uid);
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{uid}/tickets")
    public ResponseEntity<List<Ticket>> getUserTickets(@PathVariable String uid) {
        List<Ticket> tickets = ticketService.getTicketsByUser(uid);
        return ResponseEntity.ok(tickets);
    }
}
