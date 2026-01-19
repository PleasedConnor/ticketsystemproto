package com.prototype.repository;

import com.prototype.entity.MetadataFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetadataFieldMappingRepository extends JpaRepository<MetadataFieldMapping, Long> {
    @Query("SELECT m FROM MetadataFieldMapping m LEFT JOIN FETCH m.connection WHERE m.connection.id = :connectionId")
    List<MetadataFieldMapping> findByConnectionId(@Param("connectionId") Long connectionId);
    
    @Query("SELECT m FROM MetadataFieldMapping m LEFT JOIN FETCH m.connection WHERE m.connection.id = :connectionId AND m.isActive = true")
    List<MetadataFieldMapping> findByConnectionIdAndIsActiveTrue(@Param("connectionId") Long connectionId);
}

