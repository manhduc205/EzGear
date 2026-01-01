package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.request.ReplyReviewRequest;
import com.manhduc205.ezgear.dtos.request.ReviewRequest;
import com.manhduc205.ezgear.dtos.responses.ApiResponse; // Đảm bảo import ApiResponse
import com.manhduc205.ezgear.dtos.responses.ReviewResponse;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createReview(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @ModelAttribute ReviewRequest req,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        try {
            Long userId = user.getId();
            reviewService.createReview(userId, req, files);

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message(Translator.toLocale("success.review.created"))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateReview(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @ModelAttribute ReviewRequest req,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        try {
            reviewService.updateReview(user.getId(), id, req, files);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message(Translator.toLocale("success.review.updated"))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    public ResponseEntity<?> replyReview(
            @PathVariable Long id,
            @RequestBody @Valid ReplyReviewRequest req
    ) {
        try {
            reviewService.replyReview(id, req.getContent());
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message(Translator.toLocale("success.review.replied"))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
            Page<ReviewResponse> reviews = reviewService.getReviewsByProduct(productId, pageable);

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Get reviews successfully")
                    .payload(reviews)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}