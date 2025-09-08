package com.prototype.repository;

import com.prototype.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    
    Optional<UserActivity> findByUserUidAndActivityDate(String userUid, LocalDate date);
    
    List<UserActivity> findByActivityDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(DISTINCT ua.user.uid) FROM UserActivity ua WHERE ua.activityDate = ?1")
    Long countActiveUsersByDate(LocalDate date);
    
    @Query("SELECT COUNT(DISTINCT ua.user.uid) FROM UserActivity ua WHERE ua.activityDate BETWEEN ?1 AND ?2")
    Long countActiveUsersBetweenDates(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT u.location, COUNT(DISTINCT ua.user.uid) FROM UserActivity ua JOIN ua.user u WHERE ua.activityDate BETWEEN ?1 AND ?2 GROUP BY u.location")
    List<Object[]> findActiveUsersByLocation(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT u.device, COUNT(DISTINCT ua.user.uid) FROM UserActivity ua JOIN ua.user u WHERE ua.activityDate BETWEEN ?1 AND ?2 GROUP BY u.device")
    List<Object[]> findActiveUsersByDevice(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(ua.ticketsCreated) FROM UserActivity ua WHERE ua.activityDate BETWEEN ?1 AND ?2")
    Long sumTicketsCreatedBetweenDates(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(ua.messagesSent) FROM UserActivity ua WHERE ua.activityDate BETWEEN ?1 AND ?2")
    Long sumMessagesSentBetweenDates(LocalDate startDate, LocalDate endDate);
}
