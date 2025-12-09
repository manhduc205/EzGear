package com.manhduc205.ezgear.dtos.responses.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.manhduc205.ezgear.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {
    private Long id;
    private String invoiceCode;

    // Thông tin tham chiếu
    private Long orderId;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String customerAddress;

    private Long subtotal;
    private Long discountTotal;
    private Long grandTotal;
    private Long paidAmount;
    private Long remainingAmount; // Số tiền còn nợ (grandTotal - paidAmount)

    private InvoiceStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;

    private String pdfUrl;

    private List<InvoiceItemResponse> items;

    @Data
    @Builder
    public static class InvoiceItemResponse {
        private Long skuId;
        private String description; // Tên SP snapshot
        private Integer quantity;
        private Long unitPrice;
        private Long lineTotal;
    }
}