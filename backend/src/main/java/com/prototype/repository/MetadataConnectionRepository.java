package com.prototype.repository;

import com.prototype.entity.MetadataConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetadataConnectionRepository extends JpaRepository<MetadataConnection, Long> {
    List<MetadataConnection> findByIsActiveTrue();
}

