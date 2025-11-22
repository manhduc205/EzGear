package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.promotion.Promotion;
import com.manhduc205.ezgear.models.promotion.PromotionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionCategoryRepository extends JpaRepository<PromotionCategory, Long> {

    @Query("SELECT pc.categoryId FROM PromotionCategory pc WHERE pc.promotionId = :promoId")
    List<Long> findCategoryIdsByPromotionId(Long promoId);

    Optional<Promotion> deleteByPromotionId(Long id);
}

