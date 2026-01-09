package com.prototype.controller;

import com.prototype.entity.SimulationResult;
import com.prototype.entity.Ticket;
import com.prototype.repository.TicketRepository;
import com.prototype.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/simulation")
public class SimulationController {
    
    @Autowired
    private SimulationService simulationService;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    /**
     * Get tickets for simulation based on selection criteria
     */
    @PostMapping("/tickets")
    public ResponseEntity<List<Map<String, Object>>> getTicketsForSimulation(
            @RequestBody TicketSelectionRequest request) {
        try {
            List<Ticket> tickets = simulationService.getTicketsForSimulation(
                request.getSelectionMode(),
                request.getTicketIds(),
                request.getCategories(),
                request.getMaxTickets()
            );
            
            List<Map<String, Object>> result = tickets.stream().map(ticket -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ticket.getId());
                map.put("subject", ticket.getSubject());
                map.put("description", ticket.getDescription());
                map.put("priority", ticket.getPriority().toString());
                map.put("category", ticket.getCategory() != null ? ticket.getCategory().toString() : null);
                map.put("status", ticket.getStatus().toString());
                return map;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Run simulation for a batch of tickets
     */
    @PostMapping("/run")
    public ResponseEntity<SimulationRunResponse> runSimulation(
            @RequestBody SimulationRunRequest request) {
        try {
            String runId = UUID.randomUUID().toString();
            List<SimulationResult> results = new ArrayList<>();
            
            List<Ticket> tickets = simulationService.getTicketsForSimulation(
                request.getSelectionMode(),
                request.getTicketIds(),
                request.getCategories(),
                request.getMaxTickets()
            );
            
            for (Ticket ticket : tickets) {
                SimulationResult result = simulationService.simulateTicket(
                    ticket, 
                    request.getModelName()
                );
                result.setSimulationRunId(runId);
                results.add(simulationService.saveResult(result));
            }
            
            SimulationRunResponse response = new SimulationRunResponse();
            response.setRunId(runId);
            response.setTotalTickets(tickets.size());
            response.setResults(results.stream().map(this::toResultMap).collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Simulate a single ticket
     */
    @PostMapping("/simulate-ticket")
    public ResponseEntity<Map<String, Object>> simulateSingleTicket(
            @RequestBody SingleTicketSimulationRequest request) {
        try {
            Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
            
            SimulationResult result = simulationService.simulateTicket(ticket, request.getModelName());
            result.setSimulationRunId(request.getRunId());
            result = simulationService.saveResult(result);
            
            return ResponseEntity.ok(toResultMap(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get results for a simulation run
     */
    @GetMapping("/results/{runId}")
    public ResponseEntity<List<Map<String, Object>>> getResults(@PathVariable String runId) {
        try {
            List<SimulationResult> results = simulationService.getResultsByRunId(runId);
            List<Map<String, Object>> resultMaps = results.stream()
                .map(this::toResultMap)
                .collect(Collectors.toList());
            return ResponseEntity.ok(resultMaps);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update a simulation result with human review
     */
    @PutMapping("/results/{resultId}")
    public ResponseEntity<Map<String, Object>> updateResult(
            @PathVariable Long resultId,
            @RequestBody ResultUpdateRequest request) {
        try {
            SimulationResult result = simulationService.updateResult(
                resultId,
                request.getHumanMark(),
                request.getHumanNotes(),
                request.getIdealResponse()
            );
            return ResponseEntity.ok(toResultMap(result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private Map<String, Object> toResultMap(SimulationResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", result.getId());
        map.put("ticketId", result.getTicketId());
        map.put("ticketSubject", result.getTicketSubject());
        map.put("humanReply", result.getHumanReply());
        map.put("aiReply", result.getAiReply());
        map.put("aiSelfReflectionCorrect", result.getAiSelfReflectionCorrect());
        map.put("aiSelfReflectionConfidence", result.getAiSelfReflectionConfidence());
        map.put("aiNotes", result.getAiNotes());
        map.put("humanMark", result.getHumanMark() != null ? result.getHumanMark().toString() : null);
        map.put("humanNotes", result.getHumanNotes());
        map.put("idealResponse", result.getIdealResponse());
        map.put("modelName", result.getModelName());
        map.put("simulationRunId", result.getSimulationRunId());
        return map;
    }
    
    // Request/Response DTOs
    public static class TicketSelectionRequest {
        private String selectionMode;
        private List<Long> ticketIds;
        private List<String> categories;
        private Integer maxTickets;
        
        public String getSelectionMode() { return selectionMode; }
        public void setSelectionMode(String selectionMode) { this.selectionMode = selectionMode; }
        
        public List<Long> getTicketIds() { return ticketIds; }
        public void setTicketIds(List<Long> ticketIds) { this.ticketIds = ticketIds; }
        
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        
        public Integer getMaxTickets() { return maxTickets; }
        public void setMaxTickets(Integer maxTickets) { this.maxTickets = maxTickets; }
    }
    
    public static class SimulationRunRequest {
        private String selectionMode;
        private List<Long> ticketIds;
        private List<String> categories;
        private Integer maxTickets;
        private String modelName;
        
        public String getSelectionMode() { return selectionMode; }
        public void setSelectionMode(String selectionMode) { this.selectionMode = selectionMode; }
        
        public List<Long> getTicketIds() { return ticketIds; }
        public void setTicketIds(List<Long> ticketIds) { this.ticketIds = ticketIds; }
        
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        
        public Integer getMaxTickets() { return maxTickets; }
        public void setMaxTickets(Integer maxTickets) { this.maxTickets = maxTickets; }
        
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
    }
    
    public static class ResultUpdateRequest {
        private SimulationResult.HumanMark humanMark;
        private String humanNotes;
        private String idealResponse;
        
        public SimulationResult.HumanMark getHumanMark() { return humanMark; }
        public void setHumanMark(SimulationResult.HumanMark humanMark) { this.humanMark = humanMark; }
        
        public String getHumanNotes() { return humanNotes; }
        public void setHumanNotes(String humanNotes) { this.humanNotes = humanNotes; }
        
        public String getIdealResponse() { return idealResponse; }
        public void setIdealResponse(String idealResponse) { this.idealResponse = idealResponse; }
    }
    
    public static class SimulationRunResponse {
        private String runId;
        private Integer totalTickets;
        private List<Map<String, Object>> results;
        
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        
        public Integer getTotalTickets() { return totalTickets; }
        public void setTotalTickets(Integer totalTickets) { this.totalTickets = totalTickets; }
        
        public List<Map<String, Object>> getResults() { return results; }
        public void setResults(List<Map<String, Object>> results) { this.results = results; }
    }
    
    public static class SingleTicketSimulationRequest {
        private Long ticketId;
        private String modelName;
        private String runId;
        
        public Long getTicketId() { return ticketId; }
        public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
        
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
    }
}

