package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.CheckoutRequest;
import com.manhduc205.ezgear.dtos.responses.OrderResponse;

public interface CheckoutService {
    public OrderResponse checkout(CheckoutRequest req, Long userId);
}
