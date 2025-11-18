package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.CheckoutRequest;
import com.manhduc205.ezgear.dtos.responses.CheckoutResponse;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request,
                                                     @AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getId();
        CheckoutResponse response = checkoutService.checkout(request, userId);
        return ResponseEntity.ok(response);
    }
}
