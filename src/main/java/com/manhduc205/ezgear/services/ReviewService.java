package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.ReviewRequest;
import com.manhduc205.ezgear.dtos.responses.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ReviewService {
    void createReview(Long userId, ReviewRequest req, List<MultipartFile> files) throws IOException;
    void replyReview(Long reviewId, String replyContent);
    void updateReview(Long userId, Long reviewId, ReviewRequest req, List<MultipartFile> files) throws Exception;
    Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable);
}