package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.ProductPaymentRequest;

import com.manhduc205.ezgear.dtos.responses.VNPayResponse;
import com.manhduc205.ezgear.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/cod")
    public ResponseEntity<String> createCodPayment(@RequestBody ProductPaymentRequest req) {
        paymentService.createCodPayment(req);
        return ResponseEntity.ok("COD payment created successfully");
    }

    @PostMapping("/vnpay/create")
    public ResponseEntity<VNPayResponse> createPaymentVNPay(@RequestBody ProductPaymentRequest req) {
        return ResponseEntity.ok(paymentService.createPaymentVNPay(req));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<String> VNPayResult(HttpServletRequest request) {
        String result = paymentService.handleVnPayCallback(request);
        return ResponseEntity.ok(result);
    }
}
