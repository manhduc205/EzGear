package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.request.ProductPaymentRequest;
import com.manhduc205.ezgear.dtos.request.order.CreateOrderRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest; // Import class này
import com.manhduc205.ezgear.dtos.responses.VNPayResponse;
import com.manhduc205.ezgear.dtos.responses.order.OrderPlacementResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.models.order.OrderItem;
import com.manhduc205.ezgear.repositories.OrderItemRepository;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.services.*;
import com.manhduc205.ezgear.shipping.service.ShippingFeeCalculatorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductSkuRepository skuRepo;
    private final ProductStockService stockService;
    private final ShippingFeeCalculatorService shippingFeeService;
    private final VoucherService voucherService;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public OrderPlacementResponse createOrder(CreateOrderRequest req, Long userId, String paymentMethod, HttpServletRequest httpRequest) {
        // 1. Validate Input
        if (req.getCartItems() == null || req.getCartItems().isEmpty()) {
            throw new RequestException("Giỏ hàng rỗng, không thể tạo đơn hàng.");
        }

        // 2. Tính toán Subtotal & Validate Tồn kho (ZERO TRUST)
        long subtotal = 0L;
        List<OrderItem> items = new ArrayList<>();
        // Tạo thêm list này để dùng cho Voucher Service
        List<ApplyVoucherItemRequest> voucherItems = new ArrayList<>();

        for (CartItemRequest ci : req.getCartItems()) {
            ProductSKU sku = skuRepo.findById(ci.getSkuId())
                    .orElseThrow(() -> new RequestException("SKU ID " + ci.getSkuId() + " không tồn tại."));

            if (ci.getQuantity() == null || ci.getQuantity() <= 0) {
                throw new RequestException("Số lượng không hợp lệ cho sản phẩm: " + sku.getName());
            }

            int available = stockService.getAvailable(ci.getSkuId(), req.getBranchId());
            if (available < ci.getQuantity()) {
                throw new RequestException("Sản phẩm " + sku.getName() + " không đủ hàng (Còn: " + available + ").");
            }

            long unitPrice = sku.getPrice();
            long lineTotal = unitPrice * ci.getQuantity();
            subtotal += lineTotal;

            // Build OrderItem (Dùng để lưu DB)
            OrderItem item = OrderItem.builder()
                    .skuId(ci.getSkuId())
                    .productId(sku.getProduct().getId())
                    .productNameSnapshot(sku.getProduct().getName())
                    .skuNameSnapshot(sku.getName())
                    .imageUrlSnapshot(sku.getProduct().getImageUrl())
                    .quantity(ci.getQuantity())
                    .unitPrice(unitPrice)
                    .discountAmount(0L)
                    .lineTotal(lineTotal)
                    .build();
            items.add(item);

            // Build ApplyVoucherItemRequest (Dùng để tính Voucher)
            // Cần map dữ liệu từ SKU sang DTO Voucher
            ApplyVoucherItemRequest voucherItem = new ApplyVoucherItemRequest();
            voucherItem.setSkuId(sku.getId());
            voucherItem.setProductId(sku.getProduct().getId());
            // Lấy category ID từ product
            if (sku.getProduct().getCategory() != null) {
                voucherItem.setCategoryId(sku.getProduct().getCategory().getId());
            }
            voucherItem.setPrice(unitPrice);
            voucherItem.setQuantity(ci.getQuantity());

            voucherItems.add(voucherItem);
        }

        // 3. Tính phí ship
        long shippingFee = 0L;
        try {
            var shippingRes = shippingFeeService.calculateShippingFee(
                    req.getBranchId(),
                    req.getAddressId(),
                    req.getCartItems(),
                    1
            );
            if (shippingRes != null && shippingRes.getData() != null) {
                shippingFee = shippingRes.getData().getTotal();
            }
        } catch (Exception e) {
            log.error("Lỗi tính phí ship: {}", e.getMessage());
            throw new RequestException("Không thể tính phí vận chuyển lúc này.");
        }

        // 4. Tính Voucher
        long discount = 0L;
        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            // SỬA LỖI Ở ĐÂY: Truyền voucherItems thay vì items
            discount = voucherService.calculateDiscountForCheckout(
                    req.getVoucherCode(),
                    voucherItems, // Đã đổi thành list đúng kiểu dữ liệu
                    subtotal,
                    shippingFee
            );
        }

        // 5. Chốt tổng tiền
        long grandTotal = Math.max(0, subtotal + shippingFee - discount);

        // 6. Sinh Mã Order
        String orderCode = generateOrderCode();

        String orderStatus = "WAITING_PAYMENT";
        String paymentStatus = "UNPAID";

        if ("COD".equalsIgnoreCase(paymentMethod)) {
            orderStatus = "PENDING_SHIPMENT";
            paymentStatus = "PAID";
        }

        Order order = Order.builder()
                .code(orderCode)
                .userId(userId)
                .branchId(req.getBranchId())
                .subtotal(subtotal)
                .discountTotal(discount)
                .shippingFee(shippingFee)
                .grandTotal(grandTotal)
                .shippingAddressId(req.getAddressId())
                .note(req.getNote())
                .status(orderStatus)
                .paymentStatus(paymentStatus)
                .build();

        Order savedOrder = orderRepo.save(order);

        for (OrderItem it : items) {
            it.setOrder(savedOrder);
            orderItemRepo.save(it);
        }
        savedOrder.setItems(items);

        // 7. Phân nhánh Thanh toán
        if ("COD".equalsIgnoreCase(paymentMethod)) {
            // --- COD ---
            for (OrderItem it : items) {
                stockService.reduceStockDirect(it.getSkuId(), savedOrder.getBranchId(), it.getQuantity());
            }

            ProductPaymentRequest paymentReq = ProductPaymentRequest.builder()
                    .orderCode(savedOrder.getCode())
                    .amount(savedOrder.getGrandTotal())
                    .build();

            paymentService.createCodPayment(paymentReq);

            return OrderPlacementResponse.builder()
                    .orderId(savedOrder.getId())
                    .orderCode(savedOrder.getCode())
                    .status(savedOrder.getStatus())
                    .paymentUrl(null)
                    .message("Đặt hàng COD thành công")
                    .build();

        } else {
            // --- VNPAY ---
            try {
                for (OrderItem it : items) {
                    stockService.reserveStock(savedOrder.getCode(), it.getSkuId(), savedOrder.getBranchId(), it.getQuantity());
                }
            } catch (Exception e) {
                throw new RequestException("Rất tiếc, sản phẩm vừa hết hàng. Vui lòng thử lại.");
            }

            ProductPaymentRequest paymentReq = ProductPaymentRequest.builder()
                    .orderCode(savedOrder.getCode())
                    .amount(savedOrder.getGrandTotal())
                    .ipAddr(httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1")
                    .build();

            VNPayResponse vnpRes = paymentService.createPaymentVNPay(paymentReq);

            return OrderPlacementResponse.builder()
                    .orderId(savedOrder.getId())
                    .orderCode(savedOrder.getCode())
                    .status(savedOrder.getStatus())
                    .paymentUrl(vnpRes.getPaymentUrl())
                    .message("Vui lòng thanh toán qua VNPay")
                    .build();
        }
    }

    // --- HÀM PRIVATE SINH MÃ ORDER (Nội bộ) ---
    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return datePart + sb;
    }
}