-- Initialize the database with customer service ticketing system
-- This file is automatically executed when the PostgreSQL container starts for the first time

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    uid VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    location VARCHAR(255) NOT NULL,
    device VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tickets table
CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    subject VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    priority VARCHAR(50) DEFAULT 'MEDIUM',
    user_uid VARCHAR(255) REFERENCES users(uid),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ticket messages table
CREATE TABLE IF NOT EXISTS ticket_messages (
    id BIGSERIAL PRIMARY KEY,
    message TEXT NOT NULL,
    sender_type VARCHAR(50) NOT NULL,
    sender_name VARCHAR(255),
    ticket_id BIGINT REFERENCES tickets(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample users (10 fake users as requested)
INSERT INTO users (uid, name, email, location, device, phone_number) VALUES 
    ('7755668855', 'Connor Peterson', 'connor.peterson@gmail.com', 'New York, NY', 'Mac', '07755668855'),
    ('1234567890', 'Sarah Johnson', 'sarah.johnson@email.com', 'Los Angeles, CA', 'Windows PC', '01234567890'),
    ('9876543210', 'Michael Chen', 'michael.chen@company.com', 'San Francisco, CA', 'iPhone', '09876543210'),
    ('5555123456', 'Emma Rodriguez', 'emma.rodriguez@service.com', 'Chicago, IL', 'Android', '05555123456'),
    ('7777888899', 'David Wilson', 'david.wilson@business.net', 'Austin, TX', 'iPad', '07777888899'),
    ('3333444455', 'Lisa Thompson', 'lisa.thompson@work.org', 'Seattle, WA', 'MacBook', '03333444455'),
    ('6666777788', 'James Brown', 'james.brown@personal.com', 'Boston, MA', 'Chrome OS', '06666777788'),
    ('2222111100', 'Maria Garcia', 'maria.garcia@home.net', 'Miami, FL', 'Windows Laptop', '02222111100'),
    ('8888999900', 'Robert Davis', 'robert.davis@client.com', 'Denver, CO', 'Samsung Galaxy', '08888999900'),
    ('4444333322', 'Jennifer Lee', 'jennifer.lee@customer.org', 'Portland, OR', 'Linux Desktop', '04444333322')
ON CONFLICT (email) DO NOTHING;

-- Insert sample tickets
INSERT INTO tickets (subject, description, status, priority, user_uid) VALUES 
    ('My issue is not listed here', 'Query: My issue is not listed here', 'OPEN', 'MEDIUM', '7755668855'),
    ('bet is void', 'I need assistance with a transaction that is not listed here.', 'OPEN', 'HIGH', '7755668855'),
    ('Login Issues', 'Cannot access my account after password reset', 'IN_PROGRESS', 'HIGH', '1234567890'),
    ('Payment Failed', 'Credit card payment was declined but amount was charged', 'OPEN', 'URGENT', '9876543210'),
    ('App Crashes', 'Mobile app keeps crashing when I try to upload documents', 'PENDING', 'MEDIUM', '5555123456'),
    ('Account Locked', 'My account has been locked after multiple login attempts', 'RESOLVED', 'HIGH', '7777888899'),
    ('Missing Transaction', 'A transaction from last week is not showing in my history', 'OPEN', 'MEDIUM', '3333444455'),
    ('Slow Performance', 'Website is loading very slowly on all pages', 'IN_PROGRESS', 'LOW', '6666777788'),
    ('Email Notifications', 'Not receiving email notifications for important updates', 'OPEN', 'LOW', '2222111100'),
    ('Data Export', 'Need to export my data but the feature is not working', 'PENDING', 'MEDIUM', '8888999900')
ON CONFLICT DO NOTHING;

-- Insert sample messages for the first ticket (matching your screenshot)
INSERT INTO ticket_messages (message, sender_type, sender_name, ticket_id) VALUES 
    ('Hello stranger, welcome! I''m here to assist you with any inquiries about something important. How may I help you today?', 'AGENT', 'Support Agent', 1),
    ('Please select the bet that you have an issue with.', 'AGENT', 'Support Agent', 1),
    ('I need assistance with a transaction that is not listed here.', 'USER', 'Connor Peterson', 1),
    ('Query: Sorry, our virtual assistant currently experiencing some technical difficulties in processing your request. Please wait while we connect you with a live support representative who can assist you further.', 'SYSTEM', 'System', 1),
    ('Query: Name: 77****855', 'USER', 'Connor Peterson', 1),
    ('Description: Test', 'USER', 'Connor Peterson', 1),
    ('hi hi 2', 'SYSTEM', 'System', 1),
    ('Taking this for testing', 'AGENT', 'Connor Peterson', 1)
ON CONFLICT DO NOTHING;

-- Add some messages for other tickets
INSERT INTO ticket_messages (message, sender_type, sender_name, ticket_id) VALUES 
    ('Hi Sarah, I can help you with your login issues. Can you tell me what happens when you try to log in?', 'AGENT', 'Support Team', 3),
    ('I get an error message saying "Invalid credentials" even though I just reset my password.', 'USER', 'Sarah Johnson', 3),
    ('Hello Michael, I see you''re having payment issues. Let me check your transaction history.', 'AGENT', 'Billing Support', 4),
    ('The charge appeared on my card but the payment shows as failed in your system.', 'USER', 'Michael Chen', 4)
ON CONFLICT DO NOTHING;
