package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.AddToCartRequest;
import com.manhduc205.ezgear.dtos.request.CartCheckoutRequest;
import com.manhduc205.ezgear.dtos.responses.CartResponse;
import com.manhduc205.ezgear.dtos.responses.CartCheckoutPreviewResponse;
import org.springframework.stereotype.Service;

// dùng concurentHashMap an toàn hơn với đa luồng, tránh race condition
@Service
public interface CartService {
    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, AddToCartRequest req);

    CartResponse updateQuantity(Long userId, Long skuId, int quantity);

    CartResponse removeItem(Long userId, Long skuId);

    void clearCart(Long userId);

    CartCheckoutPreviewResponse previewCheckout(CartCheckoutRequest req, Long userId);
}
