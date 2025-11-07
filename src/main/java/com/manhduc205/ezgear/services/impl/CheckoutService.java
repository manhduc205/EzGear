package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.request.CheckoutRequest;
import com.manhduc205.ezgear.dtos.request.OrderItemRequest;
import com.manhduc205.ezgear.dtos.request.OrderRequest;
import com.manhduc205.ezgear.dtos.responses.OrderResponse;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.services.OrderService;
import com.manhduc205.ezgear.services.ProductStockService;
import com.manhduc205.ezgear.services.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ProductStockService stockService;
    private final PromotionService promotionService;
    private final OrderService orderService;

    @Transactional
    public OrderResponse checkout(CheckoutRequest req, Long userId) {

        // Kiểm tra tồn kho từng SKU
        for (CartItemRequest item : req.getCartItems()) {
            int available = stockService.getAvailable(item.getSkuId(), 1L); // warehouse mặc định
            if (available < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm SKU " + item.getSkuId() + " không đủ hàng tồn");
            }
        }

        //Tính tổng phụ (subtotal)
        BigDecimal subtotal = req.getCartItems().stream()
                .map(i -> {
                    BigDecimal price = i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal discount = i.getDiscountAmount() != null ? i.getDiscountAmount() : BigDecimal.ZERO;
                    return price.subtract(discount).multiply(BigDecimal.valueOf(i.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = promotionService.applyVoucher(req.getVoucherCode(), subtotal);

        BigDecimal shippingFee = req.getShippingFee() != null ? req.getShippingFee() : BigDecimal.valueOf(25000);

        BigDecimal total = subtotal.subtract(discount).add(shippingFee);

        OrderRequest orderReq = OrderRequest.builder()
                .userId(userId)
                .shippingAddressId(req.getAddressId())
                .subtotal(subtotal)
                .discountTotal(discount)
                .shippingFee(shippingFee)
                .grandTotal(total)
                .paymentMethod(req.getPaymentMethod())
                .items(req.getCartItems().stream().map(ci ->
                        OrderItemRequest.builder()
                                .skuId(ci.getSkuId())
                                .productNameSnapshot("SKU " + ci.getSkuId())
                                .skuNameSnapshot("Default")
                                .quantity(ci.getQuantity())
                                .unitPrice(ci.getUnitPrice())
                                .discountAmount(ci.getDiscountAmount())
                                .build()
                ).toList())
                .build();

        // 7️⃣ Tạo order
        Order order = orderService.createOrder(orderReq);

        // 8️⃣ Trừ tồn kho
        stockService.reduceStock(req.getCartItems(), order.getId());

        // 9️⃣ Trả về kết quả
        return OrderResponse.builder()
                .orderCode(order.getCode())
                .subtotal(subtotal)
                .discount(discount)
                .shippingFee(shippingFee)
                .total(total)
                .paymentMethod(req.getPaymentMethod())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
