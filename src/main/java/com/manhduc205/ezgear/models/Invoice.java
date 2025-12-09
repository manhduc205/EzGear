package com.manhduc205.ezgear.models;

import com.manhduc205.ezgear.enums.InvoiceStatus;
import com.manhduc205.ezgear.models.order.Order;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends AbstractEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @Column(name = "subtotal")
    private Long subtotal; // Tổng tiền hàng

    @Column(name = "discount_total")
    private Long discountTotal; // Tổng giảm giá

    @Column(name = "grand_total")
    private Long grandTotal; // Khách phải trả

    @Column(name = "paid_amount")
    private Long paidAmount; // Khách đã trả

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InvoiceStatus status;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt; // Ngày xuất hóa đơn

    @Column(name = "pdf_url")
    private String pdfUrl; // Link file PDF (S3/MinIO/Local)

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items;
}