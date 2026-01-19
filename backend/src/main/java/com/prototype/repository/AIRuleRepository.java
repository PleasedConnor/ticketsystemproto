package com.prototype.repository;

import com.prototype.entity.AIRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIRuleRepository extends JpaRepository<AIRule, Long> {
    List<AIRule> findByCategoryOrderByDisplayOrderAsc(AIRule.RuleCategory category);
    List<AIRule> findAllByOrderByCategoryAscDisplayOrderAsc();
}

