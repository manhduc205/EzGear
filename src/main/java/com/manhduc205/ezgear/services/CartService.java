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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductStockService productStockService;
    private final WarehouseService warehouseService;
    private final CustomerAddressRepository customerAddressRepository;
    // dùng concurentHashMap an toàn hơn với đa luồng, tránh race condition
    private final Map<Long, Long> warehouseCache = new ConcurrentHashMap<>();

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
//
//        CustomerAddress address = customerAddressRepository.findByUserIdAndIsDefaultTrue(userId)
//                .orElseThrow(() -> new RequestException("Bạn chưa có địa chỉ giao hàng mặc định."));
//
//        //kho tương ứng với tỉnh/thành
//        Long warehouseId = warehouseService.getWarehouseIdByAddress(address);
//        // tồn kho
//        int quantityAvailable = productStockService.getAvailable(item.getSkuId(),warehouseId);
        validateStock(userId, item.getSkuId(), item.getQuantity());
        Cart cart = getCart(userId);

        CartItem existingItem = null;
        for (CartItem it : cart.getItems()) {
            if (it.getSkuId().equals(item.getSkuId())) {
                existingItem = it;
                break;
            }
        }


        int newQuantity = (existingItem != null ? existingItem.getQuantity() : 0) + item.getQuantity();

        validateStock(userId, item.getSkuId(), newQuantity);

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
        }
        else {
            cart.getItems().add(item);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    public Cart updateQuantity(Long userId, Long skuId, int newQuantity) {
        Cart cart = getCart(userId);
        validateStock(userId, skuId, newQuantity);

        CartItem existingItem = null;
        for (CartItem it : cart.getItems()) {
            if (it.getSkuId().equals(skuId)) {
                existingItem = it;
                break;
            }
        }
        if (existingItem == null) {
            throw new RequestException("Không tìm thấy sản phẩm trong giỏ hàng.");
        }
        existingItem.setQuantity(newQuantity);
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    public Cart removeItem(Long userId, Long skuId) {
        Cart cart = getCart(userId);

//        for (Iterator<CartItem> iterator = cart.getItems().iterator(); iterator.hasNext();) {
//            CartItem item = iterator.next();
//            if (item.getSkuId().equals(skuId)) {
//                iterator.remove();
//            }
//        }

        boolean removed = cart.getItems().removeIf(i -> skuId.equals(i.getSkuId()));
        if (!removed) {
            throw new RequestException("Không tìm thấy sản phẩm trong giỏ hàng.");
        }
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    public void clearCart(Long userId) {
        Cart cart = getCart(userId);
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    // ktra tồn kho
    private void validateStock(Long userId, Long skuId, int quantity) {
        if (quantity <= 0) return;

        // key chưa tồn tại trong map -> chạy hàm mappingFunction để tính ra value mới và thêm vào map
        // key tồn tại -> lấy value hiện có, k chạy hàm nữa
        Long warehouseId = warehouseCache.computeIfAbsent(userId, id -> {
            CustomerAddress address = customerAddressRepository.findByUserIdAndIsDefaultTrue(id)
                    .orElseThrow(() -> new RequestException("Bạn chưa có địa chỉ giao hàng mặc định."));
            return warehouseService.getWarehouseIdByAddress(address);
        });

        int quantityAvailable = productStockService.getAvailable(skuId, warehouseId);
        if (quantity > quantityAvailable) {
            throw new RequestException("Không đủ tồn kho, chỉ còn " + quantityAvailable + " sản phẩm.");
        }
    }
    // xóa cache warehouseId khi user cập nhật lại địa chỉ mặc định
    public void invalidateWarehouseCache(Long userId) {
        warehouseCache.remove(userId);
    }
}

