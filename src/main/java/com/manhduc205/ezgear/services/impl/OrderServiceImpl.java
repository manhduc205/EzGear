package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.request.ProductPaymentRequest;
import com.manhduc205.ezgear.dtos.request.order.CreateOrderRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest;
import com.manhduc205.ezgear.dtos.responses.VNPayResponse;
import com.manhduc205.ezgear.dtos.responses.order.OrderListResponse;
import com.manhduc205.ezgear.dtos.responses.order.OrderPlacementResponse;
import com.manhduc205.ezgear.dtos.responses.order.OrderResponse;
import com.manhduc205.ezgear.enums.OrderStatus;
import com.manhduc205.ezgear.enums.PaymentMethod;
import com.manhduc205.ezgear.exceptions.AccessDeniedException;
import com.manhduc205.ezgear.exceptions.DataNotFoundException;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.models.order.OrderItem;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.services.*;
import com.manhduc205.ezgear.shipping.service.ShippingFeeCalculatorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductSkuRepository skuRepo;
    private final UserRepository userRepo;
    private final CustomerAddressRepository addressRepo;
    private final ProductStockService stockService;
    private final StockTransferService stockTransferService;
    private final WarehouseService warehouseService;
    private final ShippingFeeCalculatorService shippingFeeService;
    private final VoucherService voucherService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final MailService mailService;
    private final FCMService fcmService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPlacementResponse createOrder(CreateOrderRequest req, Long userId, String paymentMethod, HttpServletRequest httpRequest) {
        if (req.getCartItems() == null || req.getCartItems().isEmpty()) {
            throw new RequestException(Translator.toLocale("error.order.empty_cart"));
        }
        if (req.getAddressId() == null) {
            throw new RequestException(Translator.toLocale("error.order.shipping_address_required"));
        }
        Integer serviceId = req.getShippingServiceId();
        if (serviceId == null) {
            throw new RequestException(Translator.toLocale("error.order.shipping_service_required"));
        }
        // Lấy thông tin Địa chỉ nhận hàng (Để biết Tỉnh nào -> Tìm kho)
        CustomerAddress address = addressRepo.findByIdAndUserId(req.getAddressId(), userId)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.order.invalid_shipping_address")));

        //  CHECK TỒN KHO THEO KHU VỰC (location context)
        // Chỉ cần Tổng tồn kho trong Tỉnh > 0 là cho phép đặt.
        for (CartItemRequest ci : req.getCartItems()) {
            int availableInProvince = stockService.getAvailableInProvince(ci.getSkuId(), address.getProvinceId());
            if (availableInProvince < ci.getQuantity()) {
                ProductSKU sku = skuRepo.findById(ci.getSkuId()).orElseThrow();
                throw new RequestException(Translator.toLocale(
                        "error.product.not_enough_stock_in_area",
                        sku.getName(),
                        availableInProvince
                ));
            }
        }

        // tìm kho giao hàng (hub warehouse)
        // tự quyết định kho nào tối ưu nhất (Gần nhất + Đủ hàng nhất)
        Warehouse hubWarehouse = warehouseService.findOptimalWarehouse(address, req.getCartItems());
        Long hubWarehouseId = hubWarehouse.getId();
        Long hubBranchId = hubWarehouse.getBranch().getId();

        // Build Order Items & Voucher Data
        long subtotal = 0L;
        List<OrderItem> items = new ArrayList<>();
        List<ApplyVoucherItemRequest> voucherItems = new ArrayList<>();

        for (CartItemRequest ci : req.getCartItems()) {
            ProductSKU sku = skuRepo.findById(ci.getSkuId())
                    .orElseThrow(() -> new RequestException(Translator.toLocale(
                            "error.sku.not_found_by_id",
                            ci.getSkuId()
                    )));

            long unitPrice = sku.getPrice();
            long lineTotal = unitPrice * ci.getQuantity();
            subtotal += lineTotal;

            // Build OrderItem Entity
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

            // Build Voucher DTO
            ApplyVoucherItemRequest vItem = new ApplyVoucherItemRequest();
            vItem.setSkuId(sku.getId());
            vItem.setProductId(sku.getProduct().getId());
            if (sku.getProduct().getCategory() != null) {
                vItem.setCategoryId(sku.getProduct().getCategory().getId());
            }
            vItem.setPrice(unitPrice);
            vItem.setQuantity(ci.getQuantity());
            voucherItems.add(vItem);
        }

        // Tính phí ship (Từ Hub Warehouse -> Địa chỉ khách)
        long shippingFee = 0L;
        try {
            var shippingRes = shippingFeeService.calculateShippingFee(
                    hubBranchId, // Quan trọng: Tính từ kho Hub
                    req.getAddressId(),
                    req.getCartItems(),
                    req.getShippingServiceId()
            );
            if (shippingRes != null && shippingRes.getData() != null) {
                shippingFee = shippingRes.getData().getTotal();
            }
        } catch (Exception e) {
            log.error("Lỗi tính phí ship: {}", e.getMessage());
            throw new RequestException(Translator.toLocale("error.order.shipping_fee_unavailable"));
        }

        // Tính Voucher
        long discount = 0L;
        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            discount = voucherService.calculateDiscountForCheckout(
                    req.getVoucherCode(), voucherItems, subtotal, shippingFee
            );
        }

        long grandTotal = Math.max(0, subtotal + shippingFee - discount);

        //Lưu Order
        String orderCode = generateOrderCode();

        String orderStatus = OrderStatus.WAITING_PAYMENT.toString();
        String paymentStatus = "UNPAID";
        if (PaymentMethod.COD.toString().equalsIgnoreCase(paymentMethod)) {
            orderStatus = OrderStatus.PENDING_SHIPMENT.toString();
            paymentStatus = "PENDING"; // COD coi như chốt đơn
        }

        Order order = Order.builder()
                .code(orderCode)
                .userId(userId)
                .branchId(hubBranchId)
                .subtotal(subtotal)
                .discountTotal(discount)
                .shippingFee(shippingFee)
                .grandTotal(grandTotal)
                .shippingAddressId(req.getAddressId())
                .shippingServiceId(serviceId)
                .note(req.getNote())
                .status(orderStatus)
                .paymentStatus(paymentStatus)
                .paymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()))
                .build();

        Order savedOrder = orderRepo.save(order);

        for (OrderItem it : items) {
            it.setOrder(savedOrder);
            orderItemRepo.save(it);
        }
        savedOrder.setItems(items);

        // Xử lý Kho & Thanh toán
        // Dùng reserveStock cho 2 trường hợp vì hàm này có logic "tìm hàng từ kho khác"

        try {
            for (OrderItem it : items) {
                // Tìm hàng trên toàn tỉnh, ưu tiên Hub
                Map<Long, Integer> allocationMap = stockService.reserveStockDistributed(
                        savedOrder.getCode(),
                        it.getSkuId(),
                        it.getQuantity(),
                        hubWarehouseId,      // Kho ưu tiên (Hub)
                        address.getProvinceId() // Context (Tỉnh)
                );

                // Duyệt kết quả phân bổ
                for (Map.Entry<Long, Integer> entry : allocationMap.entrySet()) {
                    Long sourceWhId = entry.getKey(); // Kho lấy hàng
                    Integer qty = entry.getValue();   // Số lượng lấy

                    // Nếu hàng KHÔNG nằm ở Hub -> Tạo phiếu chuyển tự động về Hub
                    if (!sourceWhId.equals(hubWarehouseId)) {
                        stockTransferService.createAutoTransfer(
                                sourceWhId,     // Từ kho vệ tinh
                                hubWarehouseId, // Về kho Hub
                                it.getSkuId(),
                                qty,
                                savedOrder.getCode()
                        );
                    }
                    // Hàm reserveStockDistributed bên trong đã thực hiện giữ chỗ
                    // tại từng kho tương ứng rồi, nên ở đây không cần gọi reserveStock nữa.
                }
            }
        } catch (Exception e) {
            throw new RequestException(Translator.toLocale("error.order.items_out_of_stock_during_processing"));
        }

        // 9. Xử lý Thanh toán (Logic phân nhánh COD/VNPAY)
        String paymentUrl = null;
        String responseMessage;

        if (PaymentMethod.COD.toString().equalsIgnoreCase(paymentMethod)) {
            // --- COD ---
            stockService.commitReservation(savedOrder.getCode());

            ProductPaymentRequest paymentReq = ProductPaymentRequest.builder()
                    .orderCode(savedOrder.getCode())
                    .amount(savedOrder.getGrandTotal())
                    .build();
            paymentService.createCodPayment(paymentReq);
            mailService.sendOrderConfirmation(savedOrder);

            responseMessage = Translator.toLocale("success.order.cod_checkout");
        } else {
            // --- VNPAY ---
            ProductPaymentRequest paymentReq = ProductPaymentRequest.builder()
                    .orderCode(savedOrder.getCode())
                    .amount(savedOrder.getGrandTotal())
                    .ipAddr(httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1")
                    .build();

            VNPayResponse vnpRes = paymentService.createPaymentVNPay(paymentReq);
            paymentUrl = vnpRes.getPaymentUrl();

            responseMessage = Translator.toLocale("instruction.order.pay_with_vnpay");
        }

        // Notification
        try {
            fcmService.sendOrderNotification(savedOrder.getCode(), savedOrder.getId(), savedOrder.getGrandTotal());
        } catch (Exception e) {
            log.warn("Lỗi gửi thông báo FCM: {}", e.getMessage());
        }

        // hóa đơn
        try {
            invoiceService.createInvoice(savedOrder.getId());
        } catch (Exception e) {
            log.error("Lỗi sinh hóa đơn tự động: {}", e.getMessage());
        }

        // 12. Return duy nhất
        return OrderPlacementResponse.builder()
                .orderId(savedOrder.getId())
                .orderCode(savedOrder.getCode())
                .status(savedOrder.getStatus())
                .paymentUrl(paymentUrl)
                .message(responseMessage)
                .build();
    }

    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return datePart + sb.toString();
    }

    @Override
    public OrderResponse getOrderDetail(Long userId, String orderCode) {
        //check đơn hàng
        Order order = orderRepo.findByCode(orderCode)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.order.not_found_by_code")));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException(Translator.toLocale("error.order.access_denied"));
        }

        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductNameSnapshot()) // Lấy tên lúc mua
                        .skuName(item.getSkuNameSnapshot())         // Lấy phân loại lúc mua
                        .imageUrl(item.getImageUrlSnapshot())
                        .quantity(item.getQuantity())
                        .originalPrice(item.getUnitPrice() + item.getDiscountAmount())
                        .price(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getCode())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod().name()) // VD: COD, VNPAY
                .receiverName(order.getShippingAddress() != null ? order.getShippingAddress().getReceiverName() : "")
                .receiverPhone(order.getShippingAddress() != null ? order.getShippingAddress().getReceiverPhone() : "")
                .receiverAddress(order.getShippingAddress() != null ? order.getShippingAddress().getAddressLine() : "")

                .merchandiseSubtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .voucherDiscount(order.getDiscountTotal())
                .grandTotal(order.getGrandTotal())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderListResponse> getMyOrders(Long userId) {
        List<Order> orders = orderRepo.findByUserIdOrderByCreatedAtDesc(userId);

        return orders.stream().map(order -> {
            List<OrderListResponse.OrderListItem> itemResponses = order.getItems().stream()
                    .map(item -> OrderListResponse.OrderListItem.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductNameSnapshot())
                            .skuName(item.getSkuNameSnapshot())
                            .imageUrl(item.getImageUrlSnapshot())
                            .quantity(item.getQuantity())
                            .price(item.getUnitPrice())
                            .build())
                    .collect(Collectors.toList());

            return OrderListResponse.builder()
                    .id(order.getId())
                    .orderCode(order.getCode())
                    .status(order.getStatus())
                    .paymentStatus(order.getPaymentStatus())
                    .grandTotal(order.getGrandTotal())
                    .createdAt(order.getCreatedAt())
                    .items(itemResponses)
                    .build();
        }).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @Override
    @Transactional(readOnly = true) //Lazy list items không bị lỗi
    public List<OrderResponse> getOrdersForPicking(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new DataNotFoundException(
                Translator.toLocale("error.user.not_found")
        ));

        boolean isSysAdmin = user.getUserRoles() != null && user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getCode().equalsIgnoreCase("SYS_ADMIN"));

        Long branchId = user.getBranchId();

        if (branchId == null && !isSysAdmin) {
            return List.of();
        }

        List<String> readyStatuses = List.of(OrderStatus.PENDING_SHIPMENT.toString(), "PAID");
        List<Order> orders;

        if (isSysAdmin) {
            // có thể dùng findByStatusIn
            orders = orderRepo.findAll();
        } else {
            orders = orderRepo.findForPicking(branchId, readyStatuses);
        }

        // 3. Mapping Inline (Order -> DTO) ngay tại đây
        return orders.stream().map(order -> {
            // Map OrderItem trước
            List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                    .map(item -> OrderResponse.OrderItemResponse.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductNameSnapshot())
                            .skuName(item.getSkuNameSnapshot()) // Đã chứa Size/Màu
                            .quantity(item.getQuantity())
                            .price(item.getUnitPrice())
                            .imageUrl(item.getImageUrlSnapshot())
                            .build())
                    .collect(Collectors.toList());

            // Map Order Response
            return OrderResponse.builder()
                    .id(order.getId())
                    .orderCode(order.getCode())
                    .status(order.getStatus())
                    .paymentStatus(order.getPaymentStatus())
                    .paymentMethod(order.getPaymentMethod().name())
                    // Null check an toàn cho Address
                    .receiverName(order.getShippingAddress() != null ? order.getShippingAddress().getReceiverName() : "")
                    .receiverPhone(order.getShippingAddress() != null ? order.getShippingAddress().getReceiverPhone() : "")
                    .receiverAddress(order.getShippingAddress() != null ? order.getShippingAddress().getAddressLine() : "")
                    .grandTotal(order.getGrandTotal())
                    .createdAt(order.getCreatedAt())
                    .items(itemResponses)
                    .build();
        }).collect(Collectors.toList());
    }
}
