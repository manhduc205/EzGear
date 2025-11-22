package com.manhduc205.ezgear.models.promotion;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Promotion_Categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;
}

