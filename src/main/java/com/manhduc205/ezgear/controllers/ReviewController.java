package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.ReviewRequest;
import com.manhduc205.ezgear.dtos.responses.ReviewResponse;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.ReviewService;
import com.manhduc205.ezgear.components.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // API: Tạo đánh giá mới
    @PostMapping("")
    public ResponseEntity<String> createReview(@Valid @RequestBody ReviewRequest req, @AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getId();

        reviewService.createReview(userId, req);

        return ResponseEntity.ok(Translator.toLocale("success.review.created"));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByProduct(@PathVariable Long productId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());

        Page<ReviewResponse> reviews = reviewService.getReviewsByProduct(productId, pageable);

        return ResponseEntity.ok(reviews);
    }
}