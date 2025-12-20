package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.request.ReviewRequest;
import com.manhduc205.ezgear.dtos.responses.ReviewResponse;
import com.manhduc205.ezgear.enums.OrderStatus;
import com.manhduc205.ezgear.enums.ReviewStatus;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.models.order.OrderItem;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.services.ReviewService;
import com.manhduc205.ezgear.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;

    @Override
    @Transactional
    public void createReview(Long userId, ReviewRequest req) {
        User user = userService.getUserById(userId);

        OrderItem orderItem = orderItemRepository.findById(req.getOrderItemId())
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.review.invalid_order_item")));

        if (!orderItem.getOrder().getUserId().equals(userId)) {
            throw new RequestException(Translator.toLocale("error.review.access_denied"));
        }

        if (!OrderStatus.COMPLETED.name().equals(orderItem.getOrder().getStatus())) {
            throw new RequestException(Translator.toLocale("error.review.order_not_completed"));
        }

        if (reviewRepository.existsByOrderItemId(req.getOrderItemId())) {
            throw new RequestException(Translator.toLocale("error.review.already_exists"));
        }

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.product.not_found")));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .orderItemId(req.getOrderItemId())
                .rating(req.getRating())
                .comment(req.getComment())
                .status(ReviewStatus.APPROVED)
                .build();

        // 4. Map ảnh (Nếu có)
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            List<ReviewImage> images = req.getImageUrls().stream()
                    .map(url -> ReviewImage.builder()
                            .review(review)
                            .imageUrl(url)
                            .build())
                    .collect(Collectors.toList());
            review.setImages(images);
        }

        reviewRepository.save(review);

        updateProductRating(product);
    }

    @Override
    public Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        // Chỉ lấy các review đã APPROVED
        Page<Review> reviewPage = reviewRepository.findByProductIdAndStatus(
                productId, ReviewStatus.APPROVED, pageable
        );

        // Convert Entity -> Response DTO
        return reviewPage.map(this::toResponse);
    }

    // --- Helper Methods ---

    // Hàm tính toán lại điểm rating trung bình
    private void updateProductRating(Product product) {
        List<Review> reviews = reviewRepository.findByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED);

        if (reviews.isEmpty()) {
            product.setRatingAverage(0.0);
            product.setReviewCount(0);
        } else {
            double avg = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            double roundedAvg = Math.round(avg * 10.0) / 10.0;

            product.setRatingAverage(roundedAvg);
            product.setReviewCount(reviews.size());
        }
        productRepository.save(product);
    }

    // Hàm convert Entity sang DTO để trả về Frontend
    private ReviewResponse toResponse(Review review) {
        List<String> imgUrls = review.getImages().stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList());

        return ReviewResponse.builder()
                .id(review.getId())
                .userName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrls(imgUrls)
                .shopResponse(review.getShopResponse())
                .createdAt(review.getCreatedAt())
                .build();
    }
}