package com.spendinganalytics.repository;

import com.spendinganalytics.entity.InsightCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InsightCacheRepository extends JpaRepository<InsightCache, Long> {
    List<InsightCache> findByGeneratedAtAfter(LocalDateTime dateTime);
    void deleteByGeneratedAtBefore(LocalDateTime dateTime);
}

