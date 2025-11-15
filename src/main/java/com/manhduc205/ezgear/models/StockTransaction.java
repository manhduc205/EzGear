package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "stock_before")
    private Integer stockBefore; // Tồn kho trước khi giao dịch

    @Column(name = "stock_after")
    private Integer stockAfter;  // Tồn kho sau giao dịch

    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;

    @Column(name = "ref_type")
    private String refType; // PO, SO, TRANSFER, ADJUST...

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "created_by")
    private Long createdBy;

    public enum Direction {
        IN, OUT
    }
}
