package com.prototype.config;

import com.prototype.entity.*;
import com.prototype.repository.UserRepository;
import com.prototype.repository.TicketRepository;
import com.prototype.repository.TicketMessageRepository;
import com.prototype.repository.MessageSentimentRepository;
import com.prototype.repository.TicketResolutionRepository;
import com.prototype.repository.UserActivityRepository;
import com.prototype.repository.SystemMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;



@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TicketMessageRepository messageRepository;
    
    @Autowired
    private MessageSentimentRepository sentimentRepository;
    
    @Autowired
    private TicketResolutionRepository resolutionRepository;
    
    @Autowired
    private UserActivityRepository activityRepository;
    
    @Autowired
    private SystemMetricsRepository systemMetricsRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void run(String... args) throws Exception {
        // Only initialize seed data if the database is completely empty
        // Never delete user-created data (tickets, AI configurations, etc.)
        if (userRepository.count() == 0 && ticketRepository.count() == 0) {
            System.out.println("Database is empty. Initializing seed data...");
            initializeData();
        } else {
            System.out.println("Database already contains data. Skipping seed data initialization to preserve user-created content.");
        }
    }

    @Transactional
    private void initializeData() {
        System.out.println("Initializing sample data...");
        
        // Create users
        User connor = createUser("7755668855", "Connor Peterson", "connor.peterson@gmail.com", "New York, NY", "Mac", "07755668855");
        User sarah = createUser("1234567890", "Sarah Johnson", "sarah.johnson@email.com", "Los Angeles, CA", "Windows PC", "01234567890");
        User michael = createUser("9876543210", "Michael Chen", "michael.chen@company.com", "San Francisco, CA", "iPhone", "09876543210");
        User emma = createUser("5555123456", "Emma Rodriguez", "emma.rodriguez@service.com", "Chicago, IL", "Android", "05555123456");
        User david = createUser("7777888899", "David Wilson", "david.wilson@business.net", "Austin, TX", "iPad", "07777888899");
        User lisa = createUser("3333444455", "Lisa Thompson", "lisa.thompson@work.org", "Seattle, WA", "MacBook", "03333444455");
        User james = createUser("6666777788", "James Brown", "james.brown@personal.com", "Boston, MA", "Chrome OS", "06666777788");
        User maria = createUser("2222111100", "Maria Garcia", "maria.garcia@home.net", "Miami, FL", "Windows Laptop", "02222111100");
        User robert = createUser("8888999900", "Robert Davis", "robert.davis@client.com", "Denver, CO", "Samsung Galaxy", "08888999900");

        // Create tickets - IDs will be automatically assigned as 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        Ticket ticket1 = createTicket("My issue is not listed here", "Query: My issue is not listed here", TicketStatus.OPEN, TicketPriority.MEDIUM, TicketCategory.GENERAL, connor);
        Ticket ticket2 = createTicket("bet is void", "I need assistance with a transaction that is not listed here.", TicketStatus.OPEN, TicketPriority.HIGH, TicketCategory.WITHDRAWALS, connor);
        Ticket ticket3 = createTicket("Login Issues", "Cannot access my account after password reset", TicketStatus.IN_PROGRESS, TicketPriority.HIGH, TicketCategory.ACCOUNT, sarah);
        Ticket ticket4 = createTicket("Payment Failed", "Credit card payment was declined but amount was charged", TicketStatus.OPEN, TicketPriority.URGENT, TicketCategory.PAYMENTS, michael);
        Ticket ticket5 = createTicket("App Crashes", "Mobile app keeps crashing when I try to upload documents", TicketStatus.PENDING, TicketPriority.MEDIUM, TicketCategory.TECHNICAL, emma);
        Ticket ticket6 = createTicket("Account Locked", "My account has been locked after multiple login attempts", TicketStatus.RESOLVED, TicketPriority.HIGH, TicketCategory.ACCOUNT, david);
        Ticket ticket7 = createTicket("Missing Transaction", "A transaction from last week is not showing in my history", TicketStatus.OPEN, TicketPriority.MEDIUM, TicketCategory.DEPOSITS, lisa);
        Ticket ticket8 = createTicket("Slow Performance", "Website is loading very slowly on all pages", TicketStatus.IN_PROGRESS, TicketPriority.LOW, TicketCategory.TECHNICAL, james);
        Ticket ticket9 = createTicket("Email Notifications", "Not receiving email notifications for important updates", TicketStatus.OPEN, TicketPriority.LOW, TicketCategory.GENERAL, maria);
        Ticket ticket10 = createTicket("Data Export", "Need to export my data but the feature is not working", TicketStatus.PENDING, TicketPriority.MEDIUM, TicketCategory.TECHNICAL, robert);

        // Create messages for ticket 1 - Connor's issue
        createMessage("Hello stranger, welcome! I'm here to assist you with any inquiries about something important. How may I help you today?", SenderType.AGENT, "Support Agent", ticket1);
        createMessage("Please select the bet that you have an issue with.", SenderType.AGENT, "Support Agent", ticket1);
        createMessage("I need assistance with a transaction that is not listed here.", SenderType.USER, "Connor Peterson", ticket1);
        createMessage("Query: Sorry, our virtual assistant currently experiencing some technical difficulties in processing your request. Please wait while we connect you with a live support representative who can assist you further.", SenderType.SYSTEM, "System", ticket1);
        createMessage("Query: Name: 77****855", SenderType.USER, "Connor Peterson", ticket1);
        createMessage("Description: Test", SenderType.USER, "Connor Peterson", ticket1);
        createMessage("hi hi 2", SenderType.SYSTEM, "System", ticket1);
        createMessage("Taking this for testing", SenderType.AGENT, "Connor Peterson", ticket1);

        // Create messages for ticket 2 - Connor's bet void issue
        createMessage("Hi Connor, I can see you're reporting a voided bet. Can you provide me with the bet ID or transaction reference?", SenderType.AGENT, "Betting Support", ticket2);
        createMessage("The bet ID is BT789456123. It was placed yesterday but now shows as void.", SenderType.USER, "Connor Peterson", ticket2);
        createMessage("I'm checking our records now. It appears this bet was voided due to a technical issue with odds calculation.", SenderType.AGENT, "Betting Support", ticket2);
        createMessage("Will I get my stake back? This is quite frustrating.", SenderType.USER, "Connor Peterson", ticket2);
        createMessage("Yes, your stake of £50 will be refunded to your account within 24 hours. I apologize for the inconvenience.", SenderType.AGENT, "Betting Support", ticket2);

        // Create messages for ticket 3 - Sarah's login issues
        createMessage("Hi Sarah, I can help you with your login issues. Can you tell me what happens when you try to log in?", SenderType.AGENT, "Support Team", ticket3);
        createMessage("I get an error message saying \"Invalid credentials\" even though I just reset my password.", SenderType.USER, "Sarah Johnson", ticket3);
        createMessage("Let me check your account status. I can see the password reset was completed successfully.", SenderType.AGENT, "Support Team", ticket3);
        createMessage("I'm using the new password exactly as it was sent to my email. Still not working.", SenderType.USER, "Sarah Johnson", ticket3);
        createMessage("I'm going to send you a temporary access code. Please check your email in the next few minutes.", SenderType.AGENT, "Support Team", ticket3);

        // Create messages for ticket 4 - Michael's payment issue
        createMessage("Hello Michael, I see you're having payment issues. Let me check your transaction history.", SenderType.AGENT, "Billing Support", ticket4);
        createMessage("The charge appeared on my card but the payment shows as failed in your system.", SenderType.USER, "Michael Chen", ticket4);
        createMessage("I can see the transaction TXN-9876543. There was a communication error with the payment processor.", SenderType.AGENT, "Billing Support", ticket4);
        createMessage("So will the charge be reversed? It's been 3 days already.", SenderType.USER, "Michael Chen", ticket4);
        createMessage("I'm escalating this to our billing team immediately. You should see the reversal within 2 business days.", SenderType.AGENT, "Billing Support", ticket4);

        // Create messages for ticket 5 - Emma's app crashes
        createMessage("Hi Emma, I understand your mobile app is crashing. Which device and app version are you using?", SenderType.AGENT, "Technical Support", ticket5);
        createMessage("I'm using Android 13 on a Samsung Galaxy S21. App version 2.1.4", SenderType.USER, "Emma Rodriguez", ticket5);
        createMessage("Thank you. Are the crashes happening when you try to upload specific types of documents?", SenderType.AGENT, "Technical Support", ticket5);
        createMessage("Yes, it crashes every time I try to upload PDF files larger than 5MB.", SenderType.USER, "Emma Rodriguez", ticket5);
        createMessage("This is a known issue with version 2.1.4. Please update to version 2.1.5 which fixes this bug.", SenderType.AGENT, "Technical Support", ticket5);

        // Create messages for ticket 6 - David's account locked (resolved)
        createMessage("Hi David, I can see your account was locked due to multiple failed login attempts.", SenderType.AGENT, "Security Team", ticket6);
        createMessage("Yes, I was trying different password combinations. I forgot my password.", SenderType.USER, "David Wilson", ticket6);
        createMessage("No problem. I've unlocked your account and sent a password reset link to your email.", SenderType.AGENT, "Security Team", ticket6);
        createMessage("Perfect! I was able to log in successfully. Thank you for the quick help!", SenderType.USER, "David Wilson", ticket6);
        createMessage("You're welcome! For security, please use a strong password and consider enabling two-factor authentication.", SenderType.AGENT, "Security Team", ticket6);

        // Create messages for ticket 7 - Lisa's missing transaction
        createMessage("Hi Lisa, I'll help you locate the missing transaction. Can you provide the approximate date and amount?", SenderType.AGENT, "Account Support", ticket7);
        createMessage("It was last Tuesday, August 29th, for $127.50. Payment to MerchantXYZ.", SenderType.USER, "Lisa Thompson", ticket7);
        createMessage("I found the transaction in our system. It seems there was a delay in updating your transaction history.", SenderType.AGENT, "Account Support", ticket7);
        createMessage("Will it show up in my account now?", SenderType.USER, "Lisa Thompson", ticket7);
        createMessage("Yes, I've manually refreshed your transaction history. The payment should now be visible in your account.", SenderType.AGENT, "Account Support", ticket7);
        createMessage("Perfect! I can see it now. Thank you so much for your help!", SenderType.USER, "Lisa Thompson", ticket7);

        // Create messages for ticket 8 - James's slow performance
        createMessage("Hi James, I see you're experiencing slow website performance. Which pages are affected most?", SenderType.AGENT, "Technical Support", ticket8);
        createMessage("The dashboard and reports pages take forever to load. Sometimes over 30 seconds.", SenderType.USER, "James Brown", ticket8);
        createMessage("I'm running some diagnostics on your account. It appears there's high data volume causing the slowdown.", SenderType.AGENT, "Technical Support", ticket8);
        createMessage("Is there anything I can do to speed it up? This is affecting my daily work.", SenderType.USER, "James Brown", ticket8);
        createMessage("I'm implementing a data optimization for your account. You should see improved performance within the next hour.", SenderType.AGENT, "Technical Support", ticket8);
        createMessage("I'll also send you some tips for managing large datasets more efficiently.", SenderType.AGENT, "Technical Support", ticket8);

        // Create messages for ticket 9 - Maria's email notifications
        createMessage("Hi Maria, I understand you're not receiving email notifications. Let me check your notification settings.", SenderType.AGENT, "Support Team", ticket9);
        createMessage("I haven't received any emails for the past week, including password resets.", SenderType.USER, "Maria Garcia", ticket9);
        createMessage("I can see your email preferences are enabled. Let me check if emails are being blocked by your provider.", SenderType.AGENT, "Support Team", ticket9);
        createMessage("I checked with Gmail and they don't show any blocked emails from your domain.", SenderType.USER, "Maria Garcia", ticket9);
        createMessage("I found the issue - there was a problem with our email delivery service. I've reset your email preferences.", SenderType.AGENT, "Support Team", ticket9);
        createMessage("You should receive a test email within the next few minutes to confirm everything is working.", SenderType.AGENT, "Support Team", ticket9);

        // Create messages for ticket 10 - Robert's data export
        createMessage("Hi Robert, I see you're having trouble with data export. What format are you trying to export to?", SenderType.AGENT, "Technical Support", ticket10);
        createMessage("I need to export to CSV but the download keeps failing at around 80% completion.", SenderType.USER, "Robert Davis", ticket10);
        createMessage("This usually happens with large datasets. Let me prepare a smaller batch export for you.", SenderType.AGENT, "Technical Support", ticket10);
        createMessage("How long will that take? I need this data for a presentation tomorrow morning.", SenderType.USER, "Robert Davis", ticket10);
        createMessage("I can have the batched exports ready within 2 hours. I'll send you download links for 3 separate files.", SenderType.AGENT, "Technical Support", ticket10);
        createMessage("That works perfectly. Thank you for the quick response!", SenderType.USER, "Robert Davis", ticket10);

        // Create analytics data
        createAnalyticsData();

        System.out.println("Sample data initialization completed!");
        System.out.println("Created " + userRepository.count() + " users");
        System.out.println("Created " + ticketRepository.count() + " tickets");
        System.out.println("Created " + messageRepository.count() + " messages");
        System.out.println("Created " + sentimentRepository.count() + " sentiment records");
        System.out.println("Created " + resolutionRepository.count() + " resolution records");
        System.out.println("Created " + activityRepository.count() + " activity records");
        System.out.println("Created " + systemMetricsRepository.count() + " system metrics records");
    }

    private void createAnalyticsData() {
        System.out.println("Creating analytics data...");
        
        // Create sentiment data for all messages
        createSentimentData();
        
        // Create resolution data for all tickets
        createResolutionData();
        
        // Create user activity data
        createUserActivityData();
        
        // Create system metrics data
        createSystemMetricsData();
    }
    
    private void createSentimentData() {
        List<TicketMessage> messages = messageRepository.findAll();
        
        for (TicketMessage message : messages) {
            SentimentLabel label;
            Double score;
            Double confidence = 0.8 + (Math.random() * 0.2); // 0.8 to 1.0
            
            // Generate realistic sentiment based on message content and sender type
            String content = message.getMessage().toLowerCase();
            
            if (message.getSenderType() == SenderType.USER) {
                // Customer messages - analyze content for sentiment
                if (content.contains("thank") || content.contains("perfect") || content.contains("great") || 
                    content.contains("excellent") || content.contains("works")) {
                    label = SentimentLabel.POSITIVE;
                    score = 0.3 + (Math.random() * 0.7); // 0.3 to 1.0
                } else if (content.contains("frustrated") || content.contains("disappointed") || content.contains("failed") || 
                          content.contains("not working") || content.contains("problem") || content.contains("issue")) {
                    label = SentimentLabel.NEGATIVE;
                    score = -0.8 + (Math.random() * 0.5); // -0.8 to -0.3
                } else if (content.contains("help") || content.contains("please") || content.contains("can you")) {
                    label = SentimentLabel.NEUTRAL;
                    score = -0.1 + (Math.random() * 0.2); // -0.1 to 0.1
                } else {
                    // Random sentiment for other messages
                    double rand = Math.random();
                    if (rand < 0.3) {
                        label = SentimentLabel.NEGATIVE;
                        score = -0.8 + (Math.random() * 0.6); // -0.8 to -0.2
                    } else if (rand < 0.6) {
                        label = SentimentLabel.NEUTRAL;
                        score = -0.2 + (Math.random() * 0.4); // -0.2 to 0.2
                    } else {
                        label = SentimentLabel.POSITIVE;
                        score = 0.2 + (Math.random() * 0.6); // 0.2 to 0.8
                    }
                }
            } else {
                // Support messages are generally neutral to positive
                label = SentimentLabel.POSITIVE;
                score = 0.1 + (Math.random() * 0.4); // 0.1 to 0.5
            }
            
            MessageSentiment sentiment = new MessageSentiment(message, score, label, confidence);
            sentimentRepository.save(sentiment);
        }
    }
    
    private void createResolutionData() {
        List<Ticket> tickets = ticketRepository.findAll();
        
        String[] supportAgents = {"Alice Johnson", "Bob Smith", "Carol Davis", "David Wilson", "Emma Brown"};
        
        for (Ticket ticket : tickets) {
            TicketResolution resolution = new TicketResolution(ticket);
            
            // Manually set resolution metrics to avoid LazyInitializationException
            LocalDateTime createdAt = ticket.getCreatedAt();
            
            // Set realistic response times (first response)
            long responseMinutes = switch (ticket.getPriority()) {
                case URGENT -> 15 + (long)(Math.random() * 45); // 15-60 minutes
                case HIGH -> 60 + (long)(Math.random() * 180); // 1-4 hours
                case MEDIUM -> 240 + (long)(Math.random() * 480); // 4-12 hours
                case LOW -> 480 + (long)(Math.random() * 960); // 8-24 hours
            };
            resolution.setResponseTimeMinutes(responseMinutes);
            resolution.setFirstResponseAt(createdAt.plusMinutes(responseMinutes));
            
            // Set realistic message counts
            resolution.setTotalMessages(3 + (int)(Math.random() * 8)); // 3-10 messages
            
            // Set resolution data for resolved/closed tickets
            if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
                resolution.setResolvedBy(supportAgents[(int)(Math.random() * supportAgents.length)]);
                
                // Set resolution time based on priority
                long resolutionHours = switch (ticket.getPriority()) {
                    case URGENT -> 1 + (long)(Math.random() * 6); // 1-7 hours
                    case HIGH -> 4 + (long)(Math.random() * 20); // 4-24 hours
                    case MEDIUM -> 12 + (long)(Math.random() * 36); // 12-48 hours
                    case LOW -> 24 + (long)(Math.random() * 96); // 1-5 days
                };
                
                resolution.setResolutionTimeHours(resolutionHours);
                resolution.setResolvedAt(createdAt.plusHours(resolutionHours));
                
                if (ticket.getStatus() == TicketStatus.CLOSED) {
                    resolution.setClosedAt(createdAt.plusHours(resolutionHours + 1));
                }
                
                // Generate customer satisfaction score
                double baseScore = 4.0;
                switch (ticket.getPriority()) {
                    case URGENT:
                        if (resolutionHours > 4) baseScore -= 1.5;
                        else if (resolutionHours > 2) baseScore -= 0.5;
                        break;
                    case HIGH:
                        if (resolutionHours > 8) baseScore -= 1.0;
                        else if (resolutionHours > 4) baseScore -= 0.3;
                        break;
                    case MEDIUM:
                        if (resolutionHours > 24) baseScore -= 0.8;
                        else if (resolutionHours > 12) baseScore -= 0.2;
                        break;
                    case LOW:
                        if (resolutionHours > 72) baseScore -= 0.5;
                        break;
                }
                baseScore += (Math.random() - 0.5) * 0.6; // ±0.3 variation
                resolution.setCustomerSatisfactionScore(Math.max(1.0, Math.min(5.0, baseScore)));
            }
            
            resolutionRepository.save(resolution);
        }
    }
    
    private void createUserActivityData() {
        List<User> users = userRepository.findAll();
        LocalDate today = LocalDate.now();
        
        // Create activity data for the last 30 days
        for (int daysAgo = 0; daysAgo < 30; daysAgo++) {
            LocalDate activityDate = today.minusDays(daysAgo);
            
            for (User user : users) {
                // Not every user is active every day
                if (Math.random() < 0.3) continue; // 70% chance of being active
                
                UserActivity activity = new UserActivity(user, activityDate);
                
                // Generate realistic activity data
                if (Math.random() < 0.1) { // 10% chance of creating a ticket
                    activity.recordTicketCreated();
                }
                
                int messageCount = (int)(Math.random() * 5); // 0-4 messages
                for (int i = 0; i < messageCount; i++) {
                    activity.recordMessageSent();
                }
                
                // Record session activity
                int sessionCount = 1 + (int)(Math.random() * 3); // 1-3 sessions
                for (int i = 0; i < sessionCount; i++) {
                    long sessionDuration = 5 + (long)(Math.random() * 45); // 5-50 minutes
                    activity.recordSession(sessionDuration);
                }
                
                activityRepository.save(activity);
            }
        }
    }

    private User createUser(String uid, String name, String email, String location, String device, String phoneNumber) {
        User user = new User(uid, name, email, location, device, phoneNumber);
        return userRepository.save(user);
    }

    private Ticket createTicket(String subject, String description, TicketStatus status, TicketPriority priority, TicketCategory category, User user) {
        Ticket ticket = new Ticket();
        ticket.setId(++ticketIdCounter);  // Simple incrementing ID: 1, 2, 3, 4, 5...
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setCategory(category);
        ticket.setUser(user);
        return ticketRepository.save(ticket);
    }

    private Long ticketIdCounter = 0L;
    private Long messageIdCounter = 0L;
    
    private TicketMessage createMessage(String message, SenderType senderType, String senderName, Ticket ticket) {
        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setId(++messageIdCounter);  // Simple incrementing ID
        ticketMessage.setMessage(message);
        ticketMessage.setSenderType(senderType);
        ticketMessage.setSenderName(senderName);
        ticketMessage.setTicket(ticket);
        return messageRepository.save(ticketMessage);
    }
    
    private void createSystemMetricsData() {
        LocalDateTime now = LocalDateTime.now();
        
        // Create metrics for the last 24 hours (hourly)
        for (int hoursAgo = 23; hoursAgo >= 0; hoursAgo--) {
            LocalDateTime recordTime = now.minusHours(hoursAgo);
            
            SystemMetrics metrics = new SystemMetrics();
            metrics.setRecordedAt(recordTime);
            
            // Simulate realistic system metrics
            int hour = recordTime.getHour();
            boolean isBusinessHours = hour >= 9 && hour <= 17;
            
            // Active users - more during business hours
            int baseActiveUsers = isBusinessHours ? 15 + (int)(Math.random() * 20) : 3 + (int)(Math.random() * 8);
            metrics.setActiveUsers(baseActiveUsers);
            
            // Ticket counts based on current data
            long totalTickets = ticketRepository.count();
            long openTickets = ticketRepository.findAll().stream()
                    .mapToLong(t -> "OPEN".equals(t.getStatus().toString()) || "IN_PROGRESS".equals(t.getStatus().toString()) ? 1 : 0)
                    .sum();
            long resolvedTickets = totalTickets - openTickets;
            
            metrics.setTotalTickets((int)totalTickets);
            metrics.setOpenTickets((int)openTickets);
            metrics.setResolvedTickets((int)resolvedTickets);
            
            // Average resolution time (varies by time of day)
            double avgResolutionHours = isBusinessHours ? 
                    8.0 + (Math.random() * 16.0) :  // 8-24 hours during business
                    16.0 + (Math.random() * 32.0);  // 16-48 hours off hours
            metrics.setAverageResolutionTimeHours(Math.round(avgResolutionHours * 100.0) / 100.0);
            
            // Messages per hour
            double messagesPerHour = isBusinessHours ? 
                    25.0 + (Math.random() * 35.0) :  // 25-60 during business
                    5.0 + (Math.random() * 15.0);    // 5-20 off hours
            metrics.setMessagesPerHour(Math.round(messagesPerHour * 100.0) / 100.0);
            
            // Average sentiment (slightly better during business hours)
            double avgSentiment = isBusinessHours ?
                    0.1 + (Math.random() * 0.3) :    // 0.1 to 0.4
                    0.0 + (Math.random() * 0.2);     // 0.0 to 0.2
            metrics.setAverageSentimentScore(Math.round(avgSentiment * 100.0) / 100.0);
            
            // System performance metrics
            double cpuUsage = isBusinessHours ?
                    45.0 + (Math.random() * 30.0) :  // 45-75% during business
                    20.0 + (Math.random() * 25.0);   // 20-45% off hours
            metrics.setCpuUsagePercent(Math.round(cpuUsage * 100.0) / 100.0);
            
            double memoryUsage = isBusinessHours ?
                    55.0 + (Math.random() * 25.0) :  // 55-80% during business
                    35.0 + (Math.random() * 20.0);   // 35-55% off hours
            metrics.setMemoryUsagePercent(Math.round(memoryUsage * 100.0) / 100.0);
            
            // Response time (milliseconds)
            int responseTime = isBusinessHours ?
                    150 + (int)(Math.random() * 200) :  // 150-350ms during business
                    80 + (int)(Math.random() * 120);    // 80-200ms off hours
            metrics.setResponseTimeMs(responseTime);
            
            systemMetricsRepository.save(metrics);
        }
    }
}
