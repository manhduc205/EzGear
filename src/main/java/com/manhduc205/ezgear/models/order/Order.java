package com.manhduc205.ezgear.models.order;

import com.manhduc205.ezgear.enums.PaymentMethod;
import com.manhduc205.ezgear.models.CustomerAddress;
import jakarta.persistence.*;
import lombok.*;
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

    private String status; // CREATED, WAITING_PAYMENT, PENDING_SHIPMENT, SHIPPING, COMPLETED, CANCELLED

    @Column(name = "subtotal")
    private Long subtotal;

    @Column(name = "discount_total")
    private Long discountTotal;

    @Column(name = "shipping_fee")
    private Long shippingFee;

    @Column(name = "grand_total")
    private Long grandTotal;

    @Column(name = "shipping_address_id")
    private Long shippingAddressId;

    @Column(name = "shipping_service_id")
    private Integer shippingServiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id", insertable = false, updatable = false)
    private CustomerAddress shippingAddress;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status")
    private String paymentStatus; // UNPAID, PENDING, PAID, FAILED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = "CREATED";
        if (paymentStatus == null) paymentStatus = "UNPAID";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
