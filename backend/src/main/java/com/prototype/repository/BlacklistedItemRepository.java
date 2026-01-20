package com.prototype.repository;

import com.prototype.entity.BlacklistedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlacklistedItemRepository extends JpaRepository<BlacklistedItem, Long> {
    
    @Query("SELECT b FROM BlacklistedItem b WHERE b.integration.id = :integrationId")
    List<BlacklistedItem> findByIntegrationId(Long integrationId);
    
    @Query("SELECT b FROM BlacklistedItem b LEFT JOIN FETCH b.integration WHERE b.integration.id = :integrationId")
    List<BlacklistedItem> findByIntegrationIdWithIntegration(Long integrationId);
    
    @Query("SELECT b FROM BlacklistedItem b WHERE b.integration.id = :integrationId AND b.itemId = :itemId")
    Optional<BlacklistedItem> findByIntegrationIdAndItemId(Long integrationId, String itemId);
    
    @Query("SELECT COUNT(b) > 0 FROM BlacklistedItem b WHERE b.integration.id = :integrationId AND b.itemId = :itemId")
    boolean existsByIntegrationIdAndItemId(Long integrationId, String itemId);
    
    @Query("SELECT b FROM BlacklistedItem b WHERE b.integration.id = :integrationId AND b.itemType = :itemType")
    List<BlacklistedItem> findByIntegrationIdAndItemType(Long integrationId, String itemType);
}

