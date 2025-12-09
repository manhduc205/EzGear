package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.InvoiceSearchRequest;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.dtos.responses.invoice.InvoiceResponse;
import com.manhduc205.ezgear.services.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @GetMapping
    public ResponseEntity<?> searchInvoices(
            @ModelAttribute InvoiceSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("issuedAt").descending());
        Page<InvoiceResponse> invoices = invoiceService.searchInvoices(request, pageable);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Lấy danh sách hóa đơn thành công")
                .payload(invoices)
                .build());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .payload(invoice)
                .build());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getInvoiceByOrderId(@PathVariable Long orderId) {
        InvoiceResponse invoice = invoiceService.getInvoiceByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .payload(invoice)
                .build());
    }

    // Tạo hóa đơn thủ công (Fallback khi hệ thống tự động lỗi hoặc Admin muốn tạo lại)
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/generate/{orderId}")
    public ResponseEntity<?> generateInvoiceManually(@PathVariable Long orderId) {
        var invoice = invoiceService.createInvoice(orderId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Đã sinh hóa đơn thành công")
                .payload(invoice)
                .build());
    }

    // đáng lẽ ra là tự động từ webhook ghn nhưng cứ làm k thừa
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PutMapping("/{id}/mark-paid")
    public ResponseEntity<?> markAsPaid(@PathVariable Long id) {
        invoiceService.markInvoiceAsPaid(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Đã cập nhật trạng thái hóa đơn sang ĐÃ THANH TOÁN")
                .build());
    }
}