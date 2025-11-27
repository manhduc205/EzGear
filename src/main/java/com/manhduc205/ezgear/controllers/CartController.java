package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.CartCheckoutRequest;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.CartResponse;
import com.manhduc205.ezgear.dtos.responses.CartCheckoutPreviewResponse;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartCheckoutPreviewResponse> cartCheckout(@RequestBody CartCheckoutRequest request,
                                                                @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = user.getId();
        CartCheckoutPreviewResponse response = cartService.previewCheckout(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal CustomUserDetails user, @RequestParam(required = false) Integer provinceId) {
        Long userId = user.getId();
        return ResponseEntity.ok(cartService.getCart(userId,provinceId));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CartItemRequest request,
            @RequestParam(required = false) Integer provinceId
    ) {
        return ResponseEntity.ok(cartService.addItem(user.getId(), request, provinceId));
    }

    @PutMapping("/update/{skuId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long skuId,
            @RequestParam int quantity,
            @RequestParam(required = false) Integer provinceId
    ) {
        return ResponseEntity.ok(cartService.updateQuantity(user.getId(), skuId, quantity, provinceId));
    }

    @DeleteMapping("/remove/{skuId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long skuId,
            @RequestParam(required = false) Integer provinceId
    ) {
        return ResponseEntity.ok(cartService.removeItem(user.getId(), skuId, provinceId));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(@AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getId();
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}
