package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.promotion.Promotion;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCode(String code);
    Boolean existsByCode(String code);
    @Query("""
        SELECT p FROM Promotion p
        WHERE p.status = 'ACTIVE'
          AND p.startAt <= CURRENT_TIMESTAMP
          AND p.endAt >= CURRENT_TIMESTAMP
          AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)  
          AND p.scope IN ('ALL', 'CATEGORY')                    
        ORDER BY p.endAt ASC
    """)
    List<Promotion> findAvailableVouchers();
}
