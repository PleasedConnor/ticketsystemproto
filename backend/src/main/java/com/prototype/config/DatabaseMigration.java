package com.prototype.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigration {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private Environment environment;
    
    @PostConstruct
    public void migrate() {
        // Only run migrations for H2 database (dev profile)
        String activeProfile = environment.getProperty("spring.profiles.active", "dev");
        if (!activeProfile.equals("dev")) {
            return;
        }
        
        try {
            // Check if DISPLAY_ORDER column exists in chatbot_actions table
            String dbUrl = environment.getProperty("spring.datasource.url", "");
            if (dbUrl.contains("h2")) {
                // For H2, check if column exists and drop it
                try {
                    // Check if column exists first
                    var result = jdbcTemplate.queryForList(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'CHATBOT_ACTIONS' AND COLUMN_NAME = 'DISPLAY_ORDER'"
                    );
                    if (!result.isEmpty()) {
                        jdbcTemplate.execute("ALTER TABLE chatbot_actions DROP COLUMN display_order");
                        System.out.println("✓ Migration: Dropped display_order column from chatbot_actions table");
                    } else {
                        System.out.println("✓ Migration: display_order column already removed");
                    }
                } catch (Exception e) {
                    // Table might not exist yet, which is fine - Hibernate will create it
                    System.out.println("Migration: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Migration error: " + e.getMessage());
            // Don't fail startup if migration fails
        }
    }
}

