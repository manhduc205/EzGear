package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.request.CheckoutRequest;
import com.manhduc205.ezgear.dtos.request.OrderItemRequest;
import com.manhduc205.ezgear.dtos.request.OrderRequest;
import com.manhduc205.ezgear.dtos.responses.OrderResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final ProductStockService stockService;
    private final OrderService orderService;
    private final ProductSkuRepository productSkuRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final WarehouseService warehouseService;

    @Transactional
    public OrderResponse checkout(CheckoutRequest req, Long userId) {

        // 1. Validate giỏ hàng
        if (req.getCartItems() == null || req.getCartItems().isEmpty()) {
            throw new RequestException("Giỏ hàng trống, không thể thanh toán.");
        }

        // 2. Validate địa chỉ giao hàng
        if (req.getAddressId() == null) {
            throw new RequestException("Bạn chưa chọn địa chỉ giao hàng.");
        }

        CustomerAddress address = customerAddressRepository
                .findByIdAndUserId(req.getAddressId(), userId)
                .orElseThrow(() -> new RequestException("Địa chỉ giao hàng không hợp lệ."));

        // 3. Lấy kho theo địa chỉ
        Long warehouseId = warehouseService.getWarehouseIdByAddress(address);

        // 4. Kiểm tra tồn kho từng SKU
        for (CartItemRequest item : req.getCartItems()) {

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RequestException("Số lượng không hợp lệ cho SKU " + item.getSkuId());
            }

            int available = stockService.getAvailable(item.getSkuId(), warehouseId);

            if (available < item.getQuantity()) {
                throw new RequestException(
                        "Sản phẩm SKU " + item.getSkuId() +
                                " không đủ tồn kho (còn " + available + ")."
                );
            }
        }

        // 5. Tính subtotal từ giá SKU trong DB (KHÔNG tin dữ liệu FE gửi)
        Long subtotal = 0L;

        List<OrderItemRequest> orderItems = new ArrayList<>();

        for (CartItemRequest ci : req.getCartItems()) {

            ProductSKU sku = productSkuRepository.findById(ci.getSkuId())
                    .orElseThrow(() -> new RequestException("SKU " + ci.getSkuId() + " không tồn tại."));

            Long unitPrice = sku.getPrice() != null ? sku.getPrice() : 0L;

            Long lineTotal = unitPrice * ci.getQuantity();
            subtotal += lineTotal;

            OrderItemRequest itemReq = OrderItemRequest.builder()
                    .skuId(sku.getId())
                    .productNameSnapshot(sku.getProduct().getName())
                    .skuNameSnapshot(sku.getName())
                    .quantity(ci.getQuantity())
                    .unitPrice(unitPrice)
                    .discountAmount(0L)     // chưa làm discount
                    .build();

            orderItems.add(itemReq);
        }

        // 6. Tạo Order
        OrderRequest orderReq = OrderRequest.builder()
                .userId(userId)
                .shippingAddressId(req.getAddressId())
                .subtotal(subtotal)
                .discountTotal(0L)         // chưa hỗ trợ giảm giá
                .shippingFee(0L)           // chưa tính phí ship
                .grandTotal(subtotal)      // total = subtotal
                .paymentMethod(req.getPaymentMethod())
                .items(orderItems)
                .build();

        Order order = orderService.createOrder(orderReq);

        // 7. Trừ tồn kho
        stockService.reduceStock(req.getCartItems(), warehouseId, order.getId());

        // 8. Trả về response
        return OrderResponse.builder()
                .orderCode(order.getCode())
                .subtotal(subtotal)
                .discount(0L)
                .shippingFee(0L)
                .total(subtotal)
                .paymentMethod(req.getPaymentMethod())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
