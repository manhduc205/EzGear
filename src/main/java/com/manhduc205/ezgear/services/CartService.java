package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.CartCheckoutRequest;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.CartResponse;
import com.manhduc205.ezgear.dtos.responses.CartCheckoutPreviewResponse;

import java.util.List;

public interface CartService {
    CartResponse getCart(Long userId, Integer provinceId);

    CartResponse addItem(Long userId, CartItemRequest req, Integer provinceId);

    CartResponse updateQuantity(Long userId, Long skuId, int qty, Integer provinceId);

    CartResponse removeItem(Long userId, Long skuId, Integer provinceId);

    void clearCart(Long userId);

    CartCheckoutPreviewResponse previewCheckout(CartCheckoutRequest req, Long userId);

    void clearCartAfterCheckout(Long userId, List<Long> skuIds);
}
