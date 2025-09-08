package com.prototype.service;

import com.prototype.entity.Ticket;
import com.prototype.entity.TicketResolution;
import com.prototype.entity.TicketStatus;
import com.prototype.entity.TicketPriority;
import com.prototype.entity.User;
import com.prototype.entity.UserActivity;
import com.prototype.entity.MessageSentiment;
import com.prototype.repository.TicketRepository;
import com.prototype.repository.TicketResolutionRepository;
import com.prototype.repository.UserRepository;
import com.prototype.repository.UserActivityRepository;
import com.prototype.repository.MessageSentimentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardAnalysisService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketResolutionRepository ticketResolutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private MessageSentimentRepository messageSentimentRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AIService aiService;

    public Map<String, Object> analyzeDashboard(List<Map<String, Object>> widgets) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Extract actual dashboard data from widgets
        Map<String, Object> dashboardData = extractDashboardData(widgets);
        
        // Generate insights based on the dashboard data
        Map<String, Object> insights = generateDashboardInsights(dashboardData, widgets);
        
        // Create AI-powered summary
        String aiSummary = generateAISummary(dashboardData, insights, widgets);
        
        analysis.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        analysis.put("dashboardData", dashboardData);
        analysis.put("insights", insights);
        analysis.put("aiSummary", aiSummary);
        analysis.put("recommendations", generateDashboardRecommendations(dashboardData, insights));
        
        return analysis;
    }

    public Map<String, Object> queryDashboard(List<Map<String, Object>> widgets, String userQuery) {
        Map<String, Object> response = new HashMap<>();
        
        // Extract dashboard data for context
        Map<String, Object> dashboardData = extractDashboardData(widgets);
        
        // Generate AI response with dashboard context
        String aiResponse = generateCustomQueryResponse(dashboardData, userQuery, widgets);
        
        response.put("query", userQuery);
        response.put("response", aiResponse);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("dashboardContext", dashboardData);
        
        return response;
    }

    private Map<String, Object> extractDashboardData(List<Map<String, Object>> widgets) {
        Map<String, Object> dashboardData = new HashMap<>();
        List<Map<String, Object>> metrics = new ArrayList<>();
        List<Map<String, Object>> charts = new ArrayList<>();
        
        for (Map<String, Object> widget : widgets) {
            Map<String, Object> widgetInfo = new HashMap<>();
            widgetInfo.put("type", widget.get("type"));
            widgetInfo.put("dataSource", widget.get("dataSource"));
            widgetInfo.put("displayName", widget.get("displayName"));
            widgetInfo.put("metricLabel", widget.get("metricLabel"));
            
            Object data = widget.get("data");
            if (data != null) {
                widgetInfo.put("data", data);
                
                // Categorize widgets for better analysis
                String type = (String) widget.get("type");
                if ("metric".equals(type)) {
                    metrics.add(widgetInfo);
                } else if (type != null && (type.contains("chart") || "table".equals(type))) {
                    charts.add(widgetInfo);
                }
            }
        }
        
        dashboardData.put("totalWidgets", widgets.size());
        dashboardData.put("metrics", metrics);
        dashboardData.put("charts", charts);
        dashboardData.put("widgetsWithData", metrics.size() + charts.size());
        
        return dashboardData;
    }

    private Map<String, Object> collectDataSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        
        // Ticket metrics
        List<Ticket> allTickets = ticketRepository.findAll();
        Map<String, Object> ticketMetrics = new HashMap<>();
        ticketMetrics.put("total", allTickets.size());
        ticketMetrics.put("open", allTickets.stream().mapToInt(t -> t.getStatus() == TicketStatus.OPEN ? 1 : 0).sum());
        ticketMetrics.put("inProgress", allTickets.stream().mapToInt(t -> t.getStatus() == TicketStatus.IN_PROGRESS ? 1 : 0).sum());
        ticketMetrics.put("resolved", allTickets.stream().mapToInt(t -> t.getStatus() == TicketStatus.RESOLVED ? 1 : 0).sum());
        ticketMetrics.put("closed", allTickets.stream().mapToInt(t -> t.getStatus() == TicketStatus.CLOSED ? 1 : 0).sum());
        
        // Priority distribution
        Map<String, Long> priorityDistribution = allTickets.stream()
            .collect(Collectors.groupingBy(t -> t.getPriority().toString(), Collectors.counting()));
        ticketMetrics.put("priorityDistribution", priorityDistribution);
        
        snapshot.put("tickets", ticketMetrics);
        
        // Resolution metrics
        List<TicketResolution> resolutions = ticketResolutionRepository.findAll();
        Map<String, Object> resolutionMetrics = new HashMap<>();
        if (!resolutions.isEmpty()) {
            double avgResolutionTime = resolutions.stream()
                .filter(r -> r.getResolutionTimeHours() != null && r.getResolutionTimeHours() > 0)
                .mapToDouble(TicketResolution::getResolutionTimeHours)
                .average()
                .orElse(0.0);
            resolutionMetrics.put("averageResolutionTime", Math.round(avgResolutionTime * 100.0) / 100.0);
            resolutionMetrics.put("totalResolved", resolutions.size());
            
            double avgSatisfaction = resolutions.stream()
                .filter(r -> r.getCustomerSatisfactionScore() != null)
                .mapToDouble(TicketResolution::getCustomerSatisfactionScore)
                .average()
                .orElse(0.0);
            resolutionMetrics.put("averageSatisfaction", Math.round(avgSatisfaction * 100.0) / 100.0);
        }
        snapshot.put("resolutions", resolutionMetrics);
        
        // User metrics
        List<User> allUsers = userRepository.findAll();
        Map<String, Object> userMetrics = new HashMap<>();
        userMetrics.put("total", allUsers.size());
        
        // Device and location distribution
        Map<String, Long> deviceDistribution = allUsers.stream()
            .collect(Collectors.groupingBy(User::getDevice, Collectors.counting()));
        Map<String, Long> locationDistribution = allUsers.stream()
            .collect(Collectors.groupingBy(User::getLocation, Collectors.counting()));
        userMetrics.put("deviceDistribution", deviceDistribution);
        userMetrics.put("locationDistribution", locationDistribution);
        
        snapshot.put("users", userMetrics);
        
        // Activity metrics (last 7 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        List<UserActivity> recentActivity = userActivityRepository.findByActivityDateBetween(startDate, endDate);
        
        Map<String, Object> activityMetrics = new HashMap<>();
        activityMetrics.put("totalSessions", recentActivity.stream().mapToInt(UserActivity::getSessionCount).sum());
        activityMetrics.put("totalMessages", recentActivity.stream().mapToInt(UserActivity::getMessagesSent).sum());
        activityMetrics.put("activeUsers", recentActivity.size());
        
        snapshot.put("activity", activityMetrics);
        
        // Sentiment metrics
        List<MessageSentiment> sentiments = messageSentimentRepository.findAll();
        Map<String, Object> sentimentMetrics = new HashMap<>();
        if (!sentiments.isEmpty()) {
            double avgSentiment = sentiments.stream()
                .mapToDouble(MessageSentiment::getSentimentScore)
                .average()
                .orElse(0.0);
            sentimentMetrics.put("averageScore", Math.round(avgSentiment * 1000.0) / 1000.0);
            
            long positive = sentiments.stream().mapToLong(s -> s.getSentimentScore() > 0.1 ? 1 : 0).sum();
            long negative = sentiments.stream().mapToLong(s -> s.getSentimentScore() < -0.1 ? 1 : 0).sum();
            long neutral = sentiments.size() - positive - negative;
            
            sentimentMetrics.put("positive", positive);
            sentimentMetrics.put("negative", negative);
            sentimentMetrics.put("neutral", neutral);
        }
        snapshot.put("sentiment", sentimentMetrics);
        
        return snapshot;
    }

    private Map<String, Object> generateDashboardInsights(Map<String, Object> dashboardData, List<Map<String, Object>> widgets) {
        Map<String, Object> insights = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) dashboardData.get("metrics");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> charts = (List<Map<String, Object>>) dashboardData.get("charts");
        
        List<String> performanceInsights = new ArrayList<>();
        List<String> dataInsights = new ArrayList<>();
        List<String> trendInsights = new ArrayList<>();
        
        // Analyze metric widgets
        if (metrics != null) {
            for (Map<String, Object> metric : metrics) {
                String metricLabel = (String) metric.get("metricLabel");
                Object data = metric.get("data");
                
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metricData = (Map<String, Object>) data;
                    Object value = metricData.get("value");
                    String label = (String) metricData.get("label");
                    
                    if (value instanceof Number) {
                        double numValue = ((Number) value).doubleValue();
                        
                        // Generate insights based on metric type and value
                        if (metricLabel != null && metricLabel.contains("Resolution Time")) {
                            if (numValue > 24) {
                                performanceInsights.add("⚠️ " + label + " is " + numValue + " hours - exceeds 24-hour target");
                            } else if (numValue < 8) {
                                performanceInsights.add("✅ " + label + " is " + numValue + " hours - excellent performance");
                            } else {
                                performanceInsights.add("📊 " + label + " is " + numValue + " hours - within acceptable range");
                            }
                        } else if (metricLabel != null && metricLabel.contains("Total")) {
                            dataInsights.add("📈 " + label + ": " + numValue);
                        } else if (metricLabel != null && metricLabel.contains("Open")) {
                            if (numValue > 20) {
                                performanceInsights.add("⚠️ High number of " + label.toLowerCase() + " (" + numValue + ")");
                            } else {
                                dataInsights.add("📊 " + label + ": " + numValue);
                            }
                        } else {
                            dataInsights.add("📊 " + label + ": " + numValue);
                        }
                    }
                }
            }
        }
        
        // Analyze chart widgets
        if (charts != null) {
            for (Map<String, Object> chart : charts) {
                String displayName = (String) chart.get("displayName");
                String dataSource = (String) chart.get("dataSource");
                Object data = chart.get("data");
                
                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> chartData = (List<Map<String, Object>>) data;
                    
                    if (!chartData.isEmpty()) {
                        trendInsights.add("📊 " + displayName + " showing " + chartData.size() + " data points from " + dataSource);
                        
                        // Find highest and lowest values for additional insights
                        OptionalDouble maxValue = chartData.stream()
                            .filter(item -> item.get("value") instanceof Number)
                            .mapToDouble(item -> ((Number) item.get("value")).doubleValue())
                            .max();
                        
                        if (maxValue.isPresent()) {
                            String maxLabel = chartData.stream()
                                .filter(item -> item.get("value") instanceof Number && 
                                       ((Number) item.get("value")).doubleValue() == maxValue.getAsDouble())
                                .map(item -> (String) item.get("label"))
                                .findFirst()
                                .orElse("Unknown");
                            
                            trendInsights.add("🔝 Highest value in " + displayName + ": " + maxLabel + " (" + maxValue.getAsDouble() + ")");
                        }
                    }
                }
            }
        }
        
        // Summary insights
        List<String> summaryInsights = new ArrayList<>();
        int totalWidgets = (Integer) dashboardData.get("totalWidgets");
        int widgetsWithData = (Integer) dashboardData.get("widgetsWithData");
        
        summaryInsights.add("Dashboard displays " + totalWidgets + " widgets with " + widgetsWithData + " containing data");
        summaryInsights.add("Metrics tracked: " + (metrics != null ? metrics.size() : 0) + " key performance indicators");
        summaryInsights.add("Visual analytics: " + (charts != null ? charts.size() : 0) + " charts and tables");
        
        insights.put("performance", performanceInsights);
        insights.put("data", dataInsights);
        insights.put("trends", trendInsights);
        insights.put("summary", summaryInsights);
        
        return insights;
    }

    private Map<String, Object> generateInsights(Map<String, Object> dataSnapshot, List<Map<String, Object>> widgets) {
        Map<String, Object> insights = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> ticketMetrics = (Map<String, Object>) dataSnapshot.get("tickets");
        @SuppressWarnings("unchecked")
        Map<String, Object> resolutionMetrics = (Map<String, Object>) dataSnapshot.get("resolutions");
        @SuppressWarnings("unchecked")
        Map<String, Object> sentimentMetrics = (Map<String, Object>) dataSnapshot.get("sentiment");
        
        // Performance insights
        List<String> performanceInsights = new ArrayList<>();
        
        if (ticketMetrics != null) {
            int total = (Integer) ticketMetrics.get("total");
            int open = (Integer) ticketMetrics.get("open");
            int resolved = (Integer) ticketMetrics.get("resolved");
            
            double resolutionRate = total > 0 ? (double) resolved / total * 100 : 0;
            performanceInsights.add(String.format("Resolution rate: %.1f%% (%d resolved out of %d total tickets)", 
                resolutionRate, resolved, total));
            
            if (open > total * 0.5) {
                performanceInsights.add("⚠️ High number of open tickets - consider increasing support capacity");
            }
        }
        
        if (resolutionMetrics != null && resolutionMetrics.containsKey("averageResolutionTime")) {
            double avgResolution = (Double) resolutionMetrics.get("averageResolutionTime");
            if (avgResolution > 24) {
                performanceInsights.add("⚠️ Average resolution time exceeds 24 hours - review escalation processes");
            } else if (avgResolution < 8) {
                performanceInsights.add("✅ Excellent resolution time - team is performing well");
            }
        }
        
        insights.put("performance", performanceInsights);
        
        // Sentiment insights
        List<String> sentimentInsights = new ArrayList<>();
        if (sentimentMetrics != null && sentimentMetrics.containsKey("averageScore")) {
            double avgSentiment = (Double) sentimentMetrics.get("averageScore");
            if (avgSentiment > 0.2) {
                sentimentInsights.add("😊 Overall positive customer sentiment detected");
            } else if (avgSentiment < -0.2) {
                sentimentInsights.add("😟 Concerning negative sentiment trend - review customer communications");
            } else {
                sentimentInsights.add("😐 Neutral customer sentiment - opportunity for improvement");
            }
            
            if (sentimentMetrics.containsKey("negative")) {
                long negative = (Long) sentimentMetrics.get("negative");
                long total = (Long) sentimentMetrics.get("positive") + (Long) sentimentMetrics.get("negative") + (Long) sentimentMetrics.get("neutral");
                if (total > 0 && negative > total * 0.3) {
                    sentimentInsights.add("⚠️ High percentage of negative messages - investigate common issues");
                }
            }
        }
        insights.put("sentiment", sentimentInsights);
        
        // Trend insights
        List<String> trendInsights = new ArrayList<>();
        trendInsights.add("Dashboard contains " + widgets.size() + " active widgets");
        
        // Analyze widget types
        Map<String, Long> widgetTypes = widgets.stream()
            .collect(Collectors.groupingBy(w -> (String) w.get("type"), Collectors.counting()));
        
        String mostCommonWidget = widgetTypes.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");
        
        trendInsights.add("Most used widget type: " + mostCommonWidget);
        insights.put("trends", trendInsights);
        
        return insights;
    }

    private String generateAISummary(Map<String, Object> dashboardData, Map<String, Object> insights, List<Map<String, Object>> widgets) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this customer service dashboard data and provide a comprehensive summary:\n\n");
        
        // Add dashboard-specific data context
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) dashboardData.get("metrics");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> charts = (List<Map<String, Object>>) dashboardData.get("charts");
        
        prompt.append("CURRENT DASHBOARD METRICS:\n");
        
        if (metrics != null && !metrics.isEmpty()) {
            for (Map<String, Object> metric : metrics) {
                String metricLabel = (String) metric.get("metricLabel");
                Object data = metric.get("data");
                
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metricData = (Map<String, Object>) data;
                    Object value = metricData.get("value");
                    String label = (String) metricData.get("label");
                    
                    if (value != null && label != null) {
                        prompt.append("- ").append(label).append(": ").append(value);
                        if (metricLabel != null && metricLabel.contains("Time")) {
                            prompt.append(" hours");
                        }
                        prompt.append("\n");
                    }
                }
            }
        }
        
        if (charts != null && !charts.isEmpty()) {
            prompt.append("\nDASHBOARD VISUALIZATIONS:\n");
            for (Map<String, Object> chart : charts) {
                String displayName = (String) chart.get("displayName");
                String dataSource = (String) chart.get("dataSource");
                Object data = chart.get("data");
                
                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> chartData = (List<Map<String, Object>>) data;
                    prompt.append("- ").append(displayName).append(" (").append(dataSource).append("): ")
                           .append(chartData.size()).append(" data points\n");
                }
            }
        }
        
        // Add insights
        prompt.append("\nKEY INSIGHTS:\n");
        @SuppressWarnings("unchecked")
        List<String> performanceInsights = (List<String>) insights.get("performance");
        if (performanceInsights != null) {
            performanceInsights.forEach(insight -> prompt.append("- ").append(insight).append("\n"));
        }
        
        prompt.append("\nPlease provide a 3-4 sentence executive summary of this customer service dashboard data. Focus on the most important findings, current performance status, and actionable recommendations for improvement. Be professional and concise.");
        
        try {
            return aiService.generateResponse(prompt.toString(), "Dashboard Analysis Request");
        } catch (Exception e) {
            // Fallback summary based on dashboard data
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dashboardMetrics = (List<Map<String, Object>>) dashboardData.get("metrics");
            int totalWidgets = (Integer) dashboardData.get("totalWidgets");
            int widgetsWithData = (Integer) dashboardData.get("widgetsWithData");
            
            return "Dashboard Analysis: Your dashboard displays " + totalWidgets + " widgets with " + 
                   widgetsWithData + " containing active data. " +
                   (dashboardMetrics != null ? dashboardMetrics.size() : 0) + " key metrics are being tracked. " +
                   "Review the displayed metrics to identify trends and optimize performance.";
        }
    }

    private List<String> generateDashboardRecommendations(Map<String, Object> dashboardData, Map<String, Object> insights) {
        List<String> recommendations = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) dashboardData.get("metrics");
        @SuppressWarnings("unchecked")
        List<String> performanceInsights = (List<String>) insights.get("performance");
        
        // Generate recommendations based on dashboard metrics
        if (metrics != null) {
            for (Map<String, Object> metric : metrics) {
                String metricLabel = (String) metric.get("metricLabel");
                Object data = metric.get("data");
                
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metricData = (Map<String, Object>) data;
                    Object value = metricData.get("value");
                    
                    if (value instanceof Number) {
                        double numValue = ((Number) value).doubleValue();
                        
                        if (metricLabel != null && metricLabel.contains("Resolution Time") && numValue > 24) {
                            recommendations.add("Consider implementing automated ticket routing to reduce resolution time");
                            recommendations.add("Review support team capacity and consider adding more agents");
                        } else if (metricLabel != null && metricLabel.contains("Open") && numValue > 20) {
                            recommendations.add("High number of open tickets requires immediate attention");
                            recommendations.add("Implement ticket prioritization system to handle workload better");
                        }
                    }
                }
            }
        }
        
        // Check if performance insights suggest issues
        if (performanceInsights != null && performanceInsights.stream().anyMatch(insight -> insight.contains("⚠️"))) {
            recommendations.add("Performance issues detected - review operational processes");
        }
        
        // General dashboard recommendations
        int totalWidgets = (Integer) dashboardData.get("totalWidgets");
        int widgetsWithData = (Integer) dashboardData.get("widgetsWithData");
        
        if (widgetsWithData < totalWidgets) {
            recommendations.add("Some widgets lack data - ensure all metrics are properly configured");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Dashboard metrics are within normal ranges - continue monitoring");
            recommendations.add("Consider adding trend analysis widgets for deeper insights");
        }
        
        return recommendations;
    }

    private String generateCustomQueryResponse(Map<String, Object> dashboardData, String userQuery, List<Map<String, Object>> widgets) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant helping to analyze a customer service dashboard. ");
        prompt.append("Answer the user's question based on the dashboard data provided.\n\n");
        
        prompt.append("USER QUESTION: ").append(userQuery).append("\n\n");
        
        // Add dashboard context
        prompt.append("DASHBOARD CONTEXT:\n");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) dashboardData.get("metrics");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> charts = (List<Map<String, Object>>) dashboardData.get("charts");
        
        if (metrics != null && !metrics.isEmpty()) {
            prompt.append("Current Metrics:\n");
            for (Map<String, Object> metric : metrics) {
                String metricLabel = (String) metric.get("metricLabel");
                Object data = metric.get("data");
                
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metricData = (Map<String, Object>) data;
                    Object value = metricData.get("value");
                    String label = (String) metricData.get("label");
                    
                    if (value != null && label != null) {
                        prompt.append("- ").append(label).append(": ").append(value);
                        if (metricLabel != null && metricLabel.contains("Time")) {
                            prompt.append(" hours");
                        }
                        prompt.append("\n");
                    }
                }
            }
        }
        
        if (charts != null && !charts.isEmpty()) {
            prompt.append("\nVisual Data:\n");
            for (Map<String, Object> chart : charts) {
                String displayName = (String) chart.get("displayName");
                String dataSource = (String) chart.get("dataSource");
                Object data = chart.get("data");
                
                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> chartData = (List<Map<String, Object>>) data;
                    prompt.append("- ").append(displayName).append(" (").append(dataSource).append("): ");
                    
                    // Add chart data details
                    for (int i = 0; i < Math.min(chartData.size(), 5); i++) { // Limit to first 5 items
                        Map<String, Object> item = chartData.get(i);
                        String label = (String) item.get("label");
                        Object value = item.get("value");
                        if (label != null && value != null) {
                            prompt.append(label).append(" (").append(value).append(")");
                            if (i < Math.min(chartData.size(), 5) - 1) prompt.append(", ");
                        }
                    }
                    if (chartData.size() > 5) {
                        prompt.append("... and ").append(chartData.size() - 5).append(" more");
                    }
                    prompt.append("\n");
                }
            }
        }
        
        prompt.append("\nPlease provide a helpful, specific answer based on this dashboard data. ");
        prompt.append("If the question cannot be answered with the available data, explain what data would be needed. ");
        prompt.append("Be conversational and explain any technical terms or metrics mentioned.");
        
        try {
            return aiService.generateResponse(prompt.toString(), "Custom Dashboard Query");
        } catch (Exception e) {
            // Fallback response
            return "I understand you're asking: \"" + userQuery + "\". Based on your dashboard with " + 
                   widgets.size() + " widgets, I can see you have " +
                   (metrics != null ? metrics.size() : 0) + " metrics and " +
                   (charts != null ? charts.size() : 0) + " charts displayed. " +
                   "However, I need the AI service to be available to provide a detailed analysis. " +
                   "Please check if your dashboard contains the specific data related to your question.";
        }
    }

    private List<String> generateRecommendations(Map<String, Object> dataSnapshot, Map<String, Object> insights) {
        List<String> recommendations = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> ticketMetrics = (Map<String, Object>) dataSnapshot.get("tickets");
        @SuppressWarnings("unchecked")
        Map<String, Object> resolutionMetrics = (Map<String, Object>) dataSnapshot.get("resolutions");
        @SuppressWarnings("unchecked")
        Map<String, Object> sentimentMetrics = (Map<String, Object>) dataSnapshot.get("sentiment");
        
        if (ticketMetrics != null) {
            int total = (Integer) ticketMetrics.get("total");
            int open = (Integer) ticketMetrics.get("open");
            
            if (open > total * 0.4) {
                recommendations.add("Consider adding more support agents or extending support hours");
                recommendations.add("Review ticket prioritization to focus on high-impact issues");
            }
        }
        
        if (resolutionMetrics != null && resolutionMetrics.containsKey("averageResolutionTime")) {
            double avgResolution = (Double) resolutionMetrics.get("averageResolutionTime");
            if (avgResolution > 24) {
                recommendations.add("Implement automated ticket routing to reduce resolution time");
                recommendations.add("Create knowledge base articles for common issues");
            }
        }
        
        if (sentimentMetrics != null && sentimentMetrics.containsKey("averageScore")) {
            double avgSentiment = (Double) sentimentMetrics.get("averageScore");
            if (avgSentiment < 0) {
                recommendations.add("Implement proactive customer communication strategies");
                recommendations.add("Review and improve response templates for better customer experience");
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Continue monitoring key metrics and maintain current performance levels");
            recommendations.add("Consider implementing predictive analytics for proactive issue resolution");
        }
        
        return recommendations;
    }
}
