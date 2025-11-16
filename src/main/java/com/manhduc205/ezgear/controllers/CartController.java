package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.AddToCartRequest;
import com.manhduc205.ezgear.dtos.responses.CartResponse;
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

    @GetMapping("")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody AddToCartRequest request
    ) {
        Long userId = user.getId();
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    @PutMapping("/update/{skuId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long skuId,
            @RequestParam int quantity
    ) {
        Long userId = user.getId();
        return ResponseEntity.ok(cartService.updateQuantity(userId, skuId, quantity));
    }

    @DeleteMapping("/remove/{skuId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long skuId
    ) {
        Long userId = user.getId();
        return ResponseEntity.ok(cartService.removeItem(userId, skuId));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(@AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getId();
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}
