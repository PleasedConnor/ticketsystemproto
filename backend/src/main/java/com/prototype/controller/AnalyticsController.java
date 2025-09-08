package com.prototype.controller;

import com.prototype.service.AnalyticsService;
import com.prototype.service.DashboardAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private DashboardAnalysisService dashboardAnalysisService;

    @PostMapping("/query")
    public ResponseEntity<List<Map<String, Object>>> executeQuery(@RequestBody AnalyticsQueryRequest request) {
        try {
            List<Map<String, Object>> result = analyticsService.executeQuery(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/metrics/tickets")
    public ResponseEntity<Map<String, Object>> getTicketMetrics() {
        Map<String, Object> metrics = analyticsService.getTicketMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/users")
    public ResponseEntity<Map<String, Object>> getUserMetrics() {
        Map<String, Object> metrics = analyticsService.getUserMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/messages")
    public ResponseEntity<Map<String, Object>> getMessageMetrics() {
        Map<String, Object> metrics = analyticsService.getMessageMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/sentiment")
    public ResponseEntity<Map<String, Object>> getSentimentMetrics() {
        Map<String, Object> metrics = analyticsService.getSentimentMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/distribution/location")
    public ResponseEntity<List<Map<String, Object>>> getLocationDistribution() {
        List<Map<String, Object>> distribution = analyticsService.getLocationDistribution();
        return ResponseEntity.ok(distribution);
    }

    @GetMapping("/distribution/device")
    public ResponseEntity<List<Map<String, Object>>> getDeviceDistribution() {
        List<Map<String, Object>> distribution = analyticsService.getDeviceDistribution();
        return ResponseEntity.ok(distribution);
    }

    @GetMapping("/distribution/status")
    public ResponseEntity<List<Map<String, Object>>> getStatusDistribution() {
        List<Map<String, Object>> distribution = analyticsService.getStatusDistribution();
        return ResponseEntity.ok(distribution);
    }

    @GetMapping("/trends/daily")
    public ResponseEntity<List<Map<String, Object>>> getDailyTrends() {
        List<Map<String, Object>> trends = analyticsService.getDailyTrends();
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/trends/hourly")
    public ResponseEntity<List<Map<String, Object>>> getHourlyActivity() {
        List<Map<String, Object>> activity = analyticsService.getHourlyActivity();
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/trends/sentiment")
    public ResponseEntity<List<Map<String, Object>>> getSentimentTrends() {
        List<Map<String, Object>> trends = analyticsService.getSentimentTrends();
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/metrics/advanced")
    public ResponseEntity<Map<String, Object>> getAdvancedMetrics() {
        Map<String, Object> metrics = analyticsService.getAdvancedMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/performance")
    public ResponseEntity<List<Map<String, Object>>> getPerformanceMetrics() {
        List<Map<String, Object>> metrics = analyticsService.getPerformanceMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/resolution-time")
    public ResponseEntity<Map<String, Object>> getAverageResolutionTime() {
        Map<String, Object> metric = analyticsService.getAverageResolutionTime();
        return ResponseEntity.ok(metric);
    }

    @PostMapping("/dashboard/analyze")
    public ResponseEntity<Map<String, Object>> analyzeDashboard(@RequestBody DashboardAnalysisRequest request) {
        Map<String, Object> analysis = dashboardAnalysisService.analyzeDashboard(request.getWidgets());
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/dashboard/query")
    public ResponseEntity<Map<String, Object>> queryDashboard(@RequestBody DashboardQueryRequest request) {
        Map<String, Object> response = dashboardAnalysisService.queryDashboard(request.getWidgets(), request.getQuery());
        return ResponseEntity.ok(response);
    }

    // DTO for analytics query requests
    public static class AnalyticsQueryRequest {
        private String dataSource;
        private String metric;
        private String groupBy;
        private String filterField;
        private String filterOperator;
        private String filterValue;

        // Getters and setters
        public String getDataSource() { return dataSource; }
        public void setDataSource(String dataSource) { this.dataSource = dataSource; }

        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }

        public String getGroupBy() { return groupBy; }
        public void setGroupBy(String groupBy) { this.groupBy = groupBy; }

        public String getFilterField() { return filterField; }
        public void setFilterField(String filterField) { this.filterField = filterField; }

        public String getFilterOperator() { return filterOperator; }
        public void setFilterOperator(String filterOperator) { this.filterOperator = filterOperator; }

        public String getFilterValue() { return filterValue; }
        public void setFilterValue(String filterValue) { this.filterValue = filterValue; }
    }

    // DTO for dashboard analysis requests
    public static class DashboardAnalysisRequest {
        private List<Map<String, Object>> widgets;

        public List<Map<String, Object>> getWidgets() { return widgets; }
        public void setWidgets(List<Map<String, Object>> widgets) { this.widgets = widgets; }
    }

    // DTO for dashboard query requests
    public static class DashboardQueryRequest {
        private List<Map<String, Object>> widgets;
        private String query;

        public List<Map<String, Object>> getWidgets() { return widgets; }
        public void setWidgets(List<Map<String, Object>> widgets) { this.widgets = widgets; }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
    }
}
