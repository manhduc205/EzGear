package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.models.cart.Cart;
import com.manhduc205.ezgear.models.cart.CartItem;
import com.manhduc205.ezgear.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/{userId}/add")
    public ResponseEntity<Cart> addItem(@PathVariable Long userId, @RequestBody CartItem item) {
        return ResponseEntity.ok(cartService.addItem(userId, item));
    }

    @PutMapping("/{userId}/update/{skuId}")
    public ResponseEntity<Cart> updateQuantity(
            @PathVariable Long userId,
            @PathVariable Long skuId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(userId, skuId, quantity));
    }

    @DeleteMapping("/{userId}/remove/{skuId}")
    public ResponseEntity<Cart> removeItem(@PathVariable Long userId, @PathVariable Long skuId) {
        return ResponseEntity.ok(cartService.removeItem(userId, skuId));
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<String> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}
