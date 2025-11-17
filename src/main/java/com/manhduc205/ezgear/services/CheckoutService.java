package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.CartCheckoutRequest;
import com.manhduc205.ezgear.dtos.responses.CartCheckoutPreviewResponse;


public interface CheckoutService {
    CartCheckoutPreviewResponse previewCheckout(CartCheckoutRequest req, Long userId);
}
