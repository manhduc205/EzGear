package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // ví dụ: EZ10, FREESHIP50K

    private String type; // ORDER, PRODUCT, SHIPPING

    @Column(name = "discount_type")
    private String discountType; // PERCENT, FIXED

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "min_order")
    private BigDecimal minOrder;

    @Column(name = "max_discount")
    private BigDecimal maxDiscount;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count")
    private Integer usedCount;

    private String scope; // GLOBAL, CATEGORY, USER

    private String status; // ACTIVE, INACTIVE, EXPIRED

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
