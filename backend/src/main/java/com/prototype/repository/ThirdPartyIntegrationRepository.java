package com.prototype.repository;

import com.prototype.entity.ThirdPartyIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThirdPartyIntegrationRepository extends JpaRepository<ThirdPartyIntegration, Long> {
    
    List<ThirdPartyIntegration> findByIsActiveTrue();
    
    List<ThirdPartyIntegration> findByAiAccessibleTrueAndIsActiveTrue();
    
    // Find integrations with OAuth app credentials configured (for reuse)
    @Query("SELECT i FROM ThirdPartyIntegration i WHERE i.integrationType = :type AND i.clientId IS NOT NULL AND i.clientId != '' AND i.clientSecret IS NOT NULL AND i.clientSecret != '' ORDER BY i.updatedAt DESC")
    List<ThirdPartyIntegration> findOAuthAppConfigsByType(ThirdPartyIntegration.IntegrationType type);
    
    Optional<ThirdPartyIntegration> findByName(String name);
    
    List<ThirdPartyIntegration> findByIntegrationType(ThirdPartyIntegration.IntegrationType integrationType);
    
    @Query("SELECT i FROM ThirdPartyIntegration i WHERE i.isActive = true AND i.aiAccessible = true AND i.syncEnabled = true")
    List<ThirdPartyIntegration> findActiveIntegrationsForSync();
}

