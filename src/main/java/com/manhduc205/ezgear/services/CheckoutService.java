package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.CheckoutRequest;
import com.manhduc205.ezgear.dtos.responses.CheckoutResponse;


public interface CheckoutService {
    CheckoutResponse checkout(CheckoutRequest req, Long userId);
}
