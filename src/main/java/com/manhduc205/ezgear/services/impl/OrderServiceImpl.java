package com.manhduc205.ezgear.services.impl;


import com.manhduc205.ezgear.dtos.request.OrderItemRequest;
import com.manhduc205.ezgear.dtos.request.OrderRequest;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.models.order.OrderItem;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;

    @Override
    @Transactional
    public Order createOrder(OrderRequest request) {

        String orderCode = generateOrderCode();

        // Order entity
        Order order = Order.builder()
                .code(orderCode)
                .userId(request.getUserId())
                .status("PENDING_CONFIRM")
                .paymentStatus("UNPAID")
                .subtotal(request.getSubtotal())
                .discountTotal(request.getDiscountTotal())
                .shippingFee(request.getShippingFee())
                .grandTotal(request.getGrandTotal())
                .shippingAddressId(request.getShippingAddressId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .skuId(i.getSkuId())
                        .productNameSnapshot(i.getProductNameSnapshot())
                        .skuNameSnapshot(i.getSkuNameSnapshot())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .discountAmount(i.getDiscountAmount() == null ? null : i.getDiscountAmount())
                        .order(order)
                        .build()
                ).toList();

        order.setItems(items);

        // Lưu Order + Items
        return orderRepo.save(order);
    }

    private String generateOrderCode() {
        return System.currentTimeMillis() +
                UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}

