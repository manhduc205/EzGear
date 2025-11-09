package com.manhduc205.ezgear.services;


import com.manhduc205.ezgear.dtos.request.ProductPaymentRequest;
import com.manhduc205.ezgear.dtos.responses.VNPayResponse;
import jakarta.servlet.http.HttpServletRequest;


public interface PaymentService {
    VNPayResponse createPaymentVNPay(ProductPaymentRequest request);
    String handleVnPayCallback(HttpServletRequest request);

    void createCodPayment(ProductPaymentRequest req);
}
