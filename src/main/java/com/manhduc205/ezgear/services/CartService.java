package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.exception.RequestException;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.cart.Cart;
import com.manhduc205.ezgear.models.cart.CartItem;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.repositories.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductStockService productStockService;
    private final WarehouseService warehouseService;
    private final CustomerAddressRepository customerAddressRepository;
    public Cart getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .items(new ArrayList<>())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    public Cart addItem(Long userId, CartItem item) {

        CustomerAddress address = customerAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new RequestException("Bạn chưa có địa chỉ giao hàng mặc định."));

        //kho tương ứng với tỉnh/thành
        Long warehouseId = warehouseService.getWarehouseIdByAddress(address);
        // tồn kho
        int quantityAvailable = productStockService.getAvailable(item.getSkuId(),warehouseId);
        Cart cart = getCart(userId);

        CartItem existingItem = null;
        for (CartItem it : cart.getItems()) {
            if (it.getSkuId().equals(item.getSkuId())) {
                existingItem = it;
                break;
            }
        }

        // 4️⃣ Tính tổng số lượng sau khi thêm
        int newQuantity = (existingItem != null ? existingItem.getQuantity() : 0) + item.getQuantity();

        // 5️⃣ Kiểm tra vượt tồn kho
        if (newQuantity > quantityAvailable) {
            throw new RequestException(
                    "Không thể thêm sản phẩm, chỉ còn lại " + quantityAvailable + " sản phẩm trong kho."
            );
        }

        // 6️⃣ Nếu đã có trong giỏ → cập nhật lại số lượng
        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
        }
        // 7️⃣ Nếu chưa có → thêm mới
        else {
            cart.getItems().add(item);
        }

        // 8️⃣ Cập nhật thời gian và lưu
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    public Cart updateQuantity(Long userId, Long skuId, int newQuantity) {
        Cart cart = getCart(userId);
        for (CartItem i : cart.getItems()) {
            if (i.getSkuId().equals(skuId)) {
                i.setQuantity(newQuantity);
            }
        }
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    public Cart removeItem(Long userId, Long skuId) {
        Cart cart = getCart(userId);
        cart.getItems().removeIf(i -> i.getSkuId().equals(skuId));
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    public void clearCart(Long userId) {
        Cart cart = getCart(userId);
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }
}

