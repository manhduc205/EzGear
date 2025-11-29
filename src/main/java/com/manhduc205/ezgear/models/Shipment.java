package com.manhduc205.ezgear.models;

import com.manhduc205.ezgear.models.order.Order;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Shipments") // Tên bảng trong DB
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment extends AbstractEntity {

    // Mapping quan hệ N-1 với bảng Orders
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(length = 100)
    private String provider; // VD: GHN, GHTK

    @Column(name = "tracking_code", length = 100)
    private String trackingCode; // Mã vận đơn (quan trọng nhất)

    @Column(length = 50)
    private String status; // VD: READY_TO_PICK, DELIVERING...

    @Column(name = "fee")
    private Long fee; // Phí ship thực tế trả cho bên vận chuyển

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Tự động set thời gian khi lưu vào DB
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}