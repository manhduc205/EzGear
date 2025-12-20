package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.enums.ReviewStatus;
import com.manhduc205.ezgear.models.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderItemId(Long orderItemId);

    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    List<Review> findByProductIdAndStatus(Long productId, ReviewStatus status);
}