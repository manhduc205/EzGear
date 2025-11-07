package com.manhduc205.ezgear.models.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id")
    private Long skuId;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @Column(name = "sku_name_snapshot")
    private String skuNameSnapshot;

    private Integer quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
