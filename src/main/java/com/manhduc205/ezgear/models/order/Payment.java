package com.manhduc205.ezgear.models.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String method; // VNPAY, MOMO, COD,...

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String status; // PENDING, SUCCESS, FAILED, REFUND

    @Column(name = "vnp_txn_ref", nullable = false, length = 100, unique = true)
    private String vnpTxnRef;

    @Column(name = "provider_txn_id", length = 255)
    private String providerTxnId; // vnp_TransactionNo

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String rawPayload; // callback trả về từ VNPay

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
