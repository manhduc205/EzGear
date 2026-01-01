package com.manhduc205.ezgear.models;

import com.manhduc205.ezgear.enums.ReviewStatus;
import com.manhduc205.ezgear.models.order.OrderItem;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Review extends AbstractEntity {

    private Integer rating;
    private String comment;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewImage> images;

    // Phần phản hồi của shop
    private String shopResponse;
    private LocalDateTime shopResponseAt;

    private LocalDateTime createdAt;

}