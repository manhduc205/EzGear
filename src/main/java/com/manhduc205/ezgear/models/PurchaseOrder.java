package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "Purchase_orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PurchaseOrder extends AbstractEntity{

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "supplier_name")
    private String supplierName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(length = 50)
    private String status;

    @Column(precision = 15, scale = 2)
    private Long  subtotal;

    @Column(precision = 15, scale = 2)
    private Long  total;

    @Lob
    private String note;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items;
}
