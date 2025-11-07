package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.OrderRequest;
import com.manhduc205.ezgear.models.order.Order;

public interface OrderService {
    Order createOrder(OrderRequest request);
}
