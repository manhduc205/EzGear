package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.request.ReviewRequest;
import com.manhduc205.ezgear.dtos.responses.ReviewResponse;
import com.manhduc205.ezgear.enums.OrderStatus;
import com.manhduc205.ezgear.enums.ReviewStatus;
import com.manhduc205.ezgear.exceptions.DataNotFoundException;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.models.order.OrderItem;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.services.CloudinaryService;
import com.manhduc205.ezgear.services.ReviewService;
import com.manhduc205.ezgear.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(rollbackFor = Exception.class) // Rollback nếu upload ảnh lỗi
    public void createReview(Long userId, ReviewRequest req, List<MultipartFile> files) throws IOException {
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
                .orderItem(orderItem)
                .rating(req.getRating())
                .comment(req.getComment())
                .status(ReviewStatus.APPROVED)
                .build();

        Review savedReview = reviewRepository.save(review);

        if (files != null && !files.isEmpty()) {
            // Giới hạn tối đa 5 ảnh
            if (files.size() > 5) {
                throw new IllegalArgumentException(Translator.toLocale("error.review.too_many_images"));
            }

            List<ReviewImage> reviewImages = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.getSize() == 0) continue;

                // Validate Size > 10MB
                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new IllegalArgumentException(Translator.toLocale("error.file_size_too_large_10mb"));
                }

                // Validate Content Type
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException(Translator.toLocale("error.file_must_be_image"));
                }

                // Upload lên Cloudinary
                String imageUrl = cloudinaryService.uploadFile(file);

                // Tạo Entity ReviewImage
                ReviewImage reviewImage = ReviewImage.builder()
                        .review(savedReview) // Link với review cha
                        .imageUrl(imageUrl)
                        .build();

                reviewImages.add(reviewImage);
            }

            if (!reviewImages.isEmpty()) {
                reviewImageRepository.saveAll(reviewImages);
            }
        }

        updateProductRating(product);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReview(Long userId, Long reviewId, ReviewRequest req, List<MultipartFile> files) throws Exception {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.review.not_found")));

        if (!review.getUser().getId().equals(userId)) {
            throw new RequestException(Translator.toLocale("error.review.access_denied"));
        }
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review.setStatus(ReviewStatus.APPROVED);

        if (files != null && !files.isEmpty()) {
            //  Xóa ảnh cũ trên Cloudinary và Database
            List<ReviewImage> oldImages = review.getImages();
            for (ReviewImage img : oldImages) {
                cloudinaryService.deleteFile(img.getImageUrl());
            }
            review.getImages().clear();

            for (MultipartFile file : files) {
                if (file.getSize() == 0) continue;
                if (file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("File too large");
                String imageUrl = cloudinaryService.uploadFile(file);

                ReviewImage newImg = ReviewImage.builder()
                        .review(review)
                        .imageUrl(imageUrl)
                        .build();
                review.getImages().add(newImg);
            }
        }

        reviewRepository.save(review);

        updateProductRating(review.getProduct());
    }

    @Override
    @Transactional
    public void replyReview(Long reviewId, String replyContent) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.review.not_found")));

        review.setShopResponse(replyContent);
        review.setShopResponseAt(LocalDateTime.now());

        reviewRepository.save(review);
    }
    @Override
    public Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByProductIdAndStatus(
                productId, ReviewStatus.APPROVED, pageable
        );
        return reviewPage.map(this::toResponse);
    }


    private void updateProductRating(Product product) {
        // Lấy tất cả review đã approve của sản phẩm
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

    private ReviewResponse toResponse(Review review) {
        // Safe check null cho list ảnh
        List<String> imgUrls = (review.getImages() == null) ? new ArrayList<>() :
                review.getImages().stream()
                        .map(ReviewImage::getImageUrl)
                        .collect(Collectors.toList());

        return ReviewResponse.builder()
                .id(review.getId())
                .userName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrls(imgUrls)
                .shopResponse(review.getShopResponse())
                .shopResponseAt(review.getShopResponseAt())
                .createdAt(review.getCreatedAt())
                .build();
    }
}