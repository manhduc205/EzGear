package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.ReviewRequest;
import com.manhduc205.ezgear.dtos.responses.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    // Tạo đánh giá mới
    void createReview(Long userId, ReviewRequest request);

    // Lấy danh sách đánh giá theo sản phẩm
    Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable);
}