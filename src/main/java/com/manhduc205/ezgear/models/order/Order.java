package com.manhduc205.ezgear.models.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "branch_id")
    private Long branchId;

    private String status; // PENDING_CONFIRM, PROCESSING, COMPLETED...

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "discount_total")
    private BigDecimal discountTotal;

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee;

    @Column(name = "grand_total")
    private BigDecimal grandTotal;

    @Column(name = "shipping_address_id")
    private Long shippingAddressId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "payment_status")
    private String paymentStatus; // UNPAID, PAID, REFUNDED...

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = "PENDING_CONFIRM";
        if (paymentStatus == null) paymentStatus = "UNPAID";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
