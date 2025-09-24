package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "productskus")
@Builder
public class ProductSKU extends AbstractEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id",nullable = false)
    private Product product;

    @Column(name = "sku", unique = true)
    private String sku;

    @Column(name = "name")
    private String name;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "weight_gram")
    private Integer weightGram;

    @Column(length = 50)
    private String sizeMm;

    @Column(nullable = false)
    private Boolean isActive = true;
}
