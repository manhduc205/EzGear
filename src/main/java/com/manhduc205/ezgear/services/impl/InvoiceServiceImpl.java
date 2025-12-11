package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.request.InvoiceSearchRequest;
import com.manhduc205.ezgear.dtos.responses.invoice.InvoiceResponse;
import com.manhduc205.ezgear.enums.InvoiceStatus;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.Invoice;
import com.manhduc205.ezgear.models.InvoiceItem;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.models.order.OrderItem;
import com.manhduc205.ezgear.repositories.InvoiceRepository;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.UserRepository;
import com.manhduc205.ezgear.services.InvoiceService;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private InvoiceRepository invoiceRepository;
    private OrderRepository orderRepository;
    private UserRepository userRepository;
    @Override
    @Transactional
    // Tạo hóa đơn từ đơn hàng (Tự động xác định trạng thái PAID/PENDING)
    public Invoice createInvoice(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(Translator.toLocale("error.order.not_found")));
        User customer = null;
        if (order.getUserId() != null) {
            customer = userRepository.findById(order.getUserId()).orElse(null);
        }
        // Nếu Hóa đơn đã tồn tại cho Đơn hàng này, trả về Hóa đơn hiện có
        var existingInvoice = invoiceRepository.findByOrderId(orderId);
        if (existingInvoice.isPresent()) {
            return existingInvoice.get();
        }
        //Xác định trạng thái Hóa đơn dựa trên trạng thái thanh toán của Đơn hàng
        InvoiceStatus status;
        Long paidAmount = 0L;
        LocalDateTime issuedAt = LocalDateTime.now();
        // Nếu là VNPAY/BANK và đã PAID -> Hóa đơn PAID.
        // Nếu là COD -> Hóa đơn PENDING (Chờ thu tiền).
        boolean isOrderPaid = "PAID".equalsIgnoreCase(order.getPaymentStatus()); // Check field payment_status của Order

        if (isOrderPaid) {
            status = InvoiceStatus.PAID;
            paidAmount = order.getGrandTotal();
        } else {
            status = InvoiceStatus.PENDING;
            paidAmount = 0L;
        }
        String invoiceCode = generateInvoiceCode();


        // Build Invoice Header
        Invoice invoice = Invoice.builder()
                .code(invoiceCode)
                .order(order)
                .customer(customer)
                .subtotal(order.getSubtotal())
                .discountTotal(order.getDiscountTotal())
                .grandTotal(order.getGrandTotal())
                .paidAmount(paidAmount)
                .status(status)
                .issuedAt(issuedAt)
                .build();

        // Build Invoice Items
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for (OrderItem orderItem : order.getItems()) {
            InvoiceItem invItem = InvoiceItem.builder()
                    .invoice(invoice)
                    .skuId(orderItem.getSkuId())
                    .description(orderItem.getProductNameSnapshot() + " - " + orderItem.getSkuNameSnapshot())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getUnitPrice())
                    .totalPrice(orderItem.getLineTotal())
                    .build();
            invoiceItems.add(invItem);
        }

        invoice.setItems(invoiceItems);

        return invoiceRepository.save(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(Translator.toLocale("error.invoice.not_found")));
        return mapToResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByOrderId(Long orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException(Translator.toLocale("error.invoice.not_exists_for_order")));
        return mapToResponse(invoice);
    }
    // Cập nhật trạng thái thanh toán (Dùng cho Webhook COD)
    @Override
    public void markInvoiceAsPaid(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException(Translator.toLocale("error.invoice.not_found")));
        if (invoice.getStatus() == InvoiceStatus.PENDING) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAmount(invoice.getGrandTotal());
            invoiceRepository.save(invoice);
            log.info("Invoice {} marked as PAID via logic.", invoice.getCode());
        }
    }
    @Override
    public Page<InvoiceResponse> searchInvoices(InvoiceSearchRequest req, Pageable pageable) {
        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req.getCode() != null && !req.getCode().isBlank()) {
                predicates.add(cb.like(root.get("code"), "%" + req.getCode() + "%"));
            }
            if (req.getOrderCode() != null && !req.getOrderCode().isBlank()) {
                predicates.add(cb.like(root.get("order").get("code"), "%" + req.getOrderCode() + "%"));
            }
            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }
            if (req.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issuedAt"), req.getFromDate().atStartOfDay()));
            }
            if (req.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issuedAt"), req.getToDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return invoiceRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    private String generateInvoiceCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();

        // Retry tối đa 3 lần nếu trùng (Dù tỷ lệ trùng rất thấp)
        for (int i = 0; i < 3; i++) {
            StringBuilder sb = new StringBuilder(5);
            for (int j = 0; j < 5; j++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String code = datePart + sb.toString();

            if (!invoiceRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new RequestException(Translator.toLocale("error.invoice.generate_code_failed"));
    }

    private InvoiceResponse mapToResponse(Invoice inv) {
        List<InvoiceResponse.InvoiceItemResponse> items = inv.getItems().stream()
                .map(item -> InvoiceResponse.InvoiceItemResponse.builder()
                        .skuId(item.getSkuId())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .id(inv.getId())
                .invoiceCode(inv.getCode())
                .orderId(inv.getOrder().getId())
                .orderCode(inv.getOrder().getCode())
                // Thông tin khách lấy từ Order (vì OrderSnapshot chính xác hơn User hiện tại)
                .customerName(inv.getOrder().getShippingAddress() != null
                        ? inv.getOrder().getShippingAddress().getReceiverName()
                        : Translator.toLocale("label.customer.walk_in"))
                .subtotal(inv.getSubtotal())
                .discountTotal(inv.getDiscountTotal())
                .grandTotal(inv.getGrandTotal())
                .paidAmount(inv.getPaidAmount())
                .remainingAmount(inv.getGrandTotal() - inv.getPaidAmount())
                .status(inv.getStatus())
                .issuedAt(inv.getIssuedAt())
                .pdfUrl(inv.getPdfUrl())
                .items(items)
                .build();
    }
}
