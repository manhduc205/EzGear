package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.order.CreateOrderRequest;
import com.manhduc205.ezgear.dtos.request.order.OrderRequest;
import com.manhduc205.ezgear.dtos.responses.order.OrderListResponse;
import com.manhduc205.ezgear.dtos.responses.order.OrderPlacementResponse;
import com.manhduc205.ezgear.dtos.responses.order.OrderResponse;
import com.manhduc205.ezgear.models.order.Order;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface OrderService {
    OrderPlacementResponse createOrder(CreateOrderRequest req, Long userId, String paymentMethod, HttpServletRequest httpRequest);
    OrderResponse getOrderDetail(Long userId, String orderCode);
    List<OrderListResponse> getMyOrders(Long userId);
}
