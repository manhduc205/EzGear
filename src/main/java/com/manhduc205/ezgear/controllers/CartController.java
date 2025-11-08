package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.models.cart.Cart;
import com.manhduc205.ezgear.models.cart.CartItem;
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
    public ResponseEntity<Cart> getCart(@AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> addItem(@AuthenticationPrincipal CustomUserDetails user, @RequestBody CartItem item) {
        Long userId = user.getId();
        return ResponseEntity.ok(cartService.addItem(userId, item));
    }

    @PutMapping("/update/{skuId}")
    public ResponseEntity<Cart> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long skuId,
            @RequestParam int quantity) {
        Long userId = user.getId();
        return ResponseEntity.ok(cartService.updateQuantity(userId, skuId, quantity));
    }

    @DeleteMapping("/remove/{skuId}")
    public ResponseEntity<Cart> removeItem(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long skuId) {
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
