package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.PaymentResultRequest;
import com.manhduc205.ezgear.dtos.request.ProductPaymentRequest;
import com.manhduc205.ezgear.dtos.responses.VNPayResponse;


public interface PaymentService {
    VNPayResponse createPayment(ProductPaymentRequest request);
    String handleVnPayCallback(PaymentResultRequest request);
}
