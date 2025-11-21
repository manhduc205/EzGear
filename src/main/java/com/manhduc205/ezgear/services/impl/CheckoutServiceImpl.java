package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.request.CheckoutRequest;
import com.manhduc205.ezgear.dtos.request.ProductPaymentRequest;
import com.manhduc205.ezgear.dtos.responses.*;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.models.order.OrderItem;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.services.*;
import com.manhduc205.ezgear.shipping.dto.response.GhnShippingFeeResponse;
import com.manhduc205.ezgear.shipping.service.ShippingFeeCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final ProductStockService productStockService;
    private final WarehouseService warehouseService;
    private final ProductSkuRepository productSkuRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final ShippingFeeCalculatorService shippingFeeCalculatorService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final CustomerAddressService customerAddressService;

    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest req, Long userId) {
        if (req.getCartItems() == null || req.getCartItems().isEmpty()) {
            throw new RequestException("Giỏ hàng trống, không thể thanh toán.");
        }

        if (req.getAddressId() == null) {
            throw new RequestException("Bạn chưa chọn địa chỉ giao hàng.");
        }

        if (req.getServiceId() == null) {
            throw new RequestException("Bạn chưa chọn phương thức vận chuyển.");
        }

        // 1) Lấy địa chỉ
        CustomerAddress address = customerAddressRepository
                .findByIdAndUserId(req.getAddressId(), userId)
                .orElseThrow(() -> new RequestException("Địa chỉ giao hàng không hợp lệ."));

        // 2) Chọn kho phù hợp từ địa chỉ
        Warehouse warehouse = warehouseService.resolveWarehouseForAddress(address);
        Long warehouseId = warehouse.getId();
        Long branchId = (warehouse.getBranch() != null) ? warehouse.getBranch().getId() : null;

        if (branchId == null) {
            throw new RequestException("Kho không gắn với chi nhánh hợp lệ.");
        }

        // 3) Kiểm tra tồn kho theo kho (warehouseId)
        for (CartItemRequest item : req.getCartItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RequestException("Số lượng không hợp lệ cho SKU " + item.getSkuId());
            }
            int available = productStockService.getAvailable(item.getSkuId(), warehouseId);
            if (available < item.getQuantity()) {
                throw new RequestException(
                        "Sản phẩm SKU " + item.getSkuId()
                                + " không đủ tồn kho (còn " + available + ")."
                );
            }
        }

        // 4) Tính subtotal + build orderItems + item preview
        long itemsSubtotal = 0L;
        List<OrderItem> orderItems = new ArrayList<>();
        List<CheckoutItemPreviewResponse> itemPreviews = new ArrayList<>();

        for (CartItemRequest ci : req.getCartItems()) {
            ProductSKU sku = productSkuRepository.findById(ci.getSkuId())
                    .orElseThrow(() -> new RequestException("SKU " + ci.getSkuId() + " không tồn tại."));

            Long unitPrice = sku.getPrice() != null ? sku.getPrice() : 0L;
            long lineTotal = unitPrice * ci.getQuantity();
            itemsSubtotal += lineTotal;

            Product product = sku.getProduct();

            OrderItem oi = OrderItem.builder()
                    .skuId(sku.getId())
                    .productId(product != null ? product.getId() : null)
                    .productNameSnapshot(product != null ? product.getName() : null)
                    .skuNameSnapshot(sku.getName())
                    .imageUrlSnapshot(product != null ? product.getImageUrl() : null)
                    .quantity(ci.getQuantity())
                    .unitPrice(unitPrice)
                    .discountAmount(0L)
                    .lineTotal(lineTotal)
                    .build();
            orderItems.add(oi);

            itemPreviews.add(CheckoutItemPreviewResponse.builder()
                    .skuId(sku.getId())
                    .productName(product != null ? product.getName() : null)
                    .skuName(sku.getName())
                    .imageUrl(product != null ? product.getImageUrl() : null)
                    .price(unitPrice)
                    .quantity(ci.getQuantity())
                    .lineTotal(lineTotal)
                    .selected(true)
                    .build());
        }

        // 5) Tính phí ship theo GHN (dùng branchId làm điểm gửi)
        Long firstSkuId = req.getCartItems().get(0).getSkuId();
        GhnShippingFeeResponse feeRes = shippingFeeCalculatorService
                .calculateShippingFee(branchId, address.getId(), firstSkuId, req.getServiceId());

        long shippingFee = 0L;
        if (feeRes.getData() != null && feeRes.getData().getTotal() != null) {
            shippingFee = feeRes.getData().getTotal();
        }

        log.info("branchId={}, warehouseId={}, addressId={}, firstSkuId={}, shippingFee={}",
                branchId,
                warehouseId,
                address.getId(),
                firstSkuId,
                shippingFee
        );

        // 6) Voucher (tạm hardcode)
        long discount = 0L;
        String voucherCode = "";
        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            // TODO: sau này check bảng Promotion
            discount = 50_000L;
            voucherCode = req.getVoucherCode();
        }

        long grandTotal = itemsSubtotal + shippingFee - discount;
        if (grandTotal < 0) grandTotal = 0;

        // 7) Tạo Order
        String orderCode = generateOrderCode();
        Order order = Order.builder()
                .code(orderCode)
                .userId(userId)
                .branchId(branchId)              // chi nhánh xuất hàng
                .status("PENDING_CONFIRM")
                .paymentStatus("UNPAID")
                .subtotal(itemsSubtotal)
                .discountTotal(discount)
                .shippingFee(shippingFee)
                .grandTotal(grandTotal)
                .shippingAddressId(address.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        orderItems.forEach(oi -> oi.setOrder(order));
        order.setItems(orderItems);
        orderRepository.save(order);

        // 8) Xử lý thanh toán
        String method = req.getPaymentMethod() != null
                ? req.getPaymentMethod().toUpperCase()
                : "COD";

        ProductPaymentRequest payReq = new ProductPaymentRequest();
        payReq.setOrderCode(orderCode);
        payReq.setAmount(grandTotal);

        String paymentUrl = null;
        if ("VNPAY".equals(method)) {
            VNPayResponse vnpRes = paymentService.createPaymentVNPay(payReq);
            paymentUrl = vnpRes.getPaymentUrl();
            order.setPaymentStatus("PENDING");
            orderRepository.save(order);
        } else if ("COD".equals(method)) {
            paymentService.createCodPayment(payReq);
            order.setStatus("PENDING_CONFIRM");
            order.setPaymentStatus("PENDING");
            orderRepository.save(order);
        } else {
            throw new RequestException("Phương thức thanh toán không được hỗ trợ.");
        }

        OrderPreviewResponse preview = OrderPreviewResponse.builder()
                .items(itemPreviews)
                .subtotal(itemsSubtotal)
                .discount(discount)
                .shippingFee(shippingFee)
                .grandTotal(grandTotal)
                .build();

        ShippingAddressInfo addressInfo = ShippingAddressInfo.builder()
                .id(address.getId())
                .fullAddress(customerAddressService.getFullAddress(address)) // hiện tại có thể = addressLine
                .isDefault(Boolean.TRUE.equals(address.getIsDefault()))
                .build();

        VoucherInfo voucherInfo = VoucherInfo.builder()
                .code(voucherCode)
                .discountValue(discount)
                .build();

        WarehouseInfo warehouseInfo = WarehouseInfo.builder()
                .id(warehouseId)
                .name(warehouse.getName())
                .build();

        return CheckoutResponse.builder()
                .orderCode(orderCode)
                .orderPreview(preview)
                .shippingAddress(addressInfo)
                .voucher(voucherInfo)
                .warehouse(warehouseInfo)
                .paymentMethod(method)
                .paymentUrl(paymentUrl)
                .message("VNPAY".equals(method)
                        ? "Vui lòng chuyển hướng tới cổng thanh toán."
                        : "Đặt hàng thành công. Thanh toán khi nhận hàng.")
                .build();
    }

    private String generateOrderCode() {
        return System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}
