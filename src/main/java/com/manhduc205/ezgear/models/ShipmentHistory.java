package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Shipment_History")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với Shipment (Nhiều lịch sử cho 1 vận đơn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Column(length = 50)
    private String status; // PICKING, DELIVERING, DELIVERED

    @Column(columnDefinition = "TEXT")
    private String note; // Mô tả sự kiện

    @Column(name = "event_time")
    private LocalDateTime eventTime; // Thời điểm xảy ra sự kiện

    @PrePersist
    protected void onCreate() {
        if (eventTime == null) {
            eventTime = LocalDateTime.now();
        }
    }
}