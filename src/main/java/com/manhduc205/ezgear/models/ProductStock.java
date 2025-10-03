package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "productstock",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_sku_wh", columnNames = {"sku_id", "warehouse_id"}) // fk trong db
)
public class ProductStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSKU productSku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "qty_on_hand", nullable = false)
    private Integer qtyOnHand = 0;

    @Column(name = "qty_reserved", nullable = false)
    private Integer qtyReserved = 0;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock = 0;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
