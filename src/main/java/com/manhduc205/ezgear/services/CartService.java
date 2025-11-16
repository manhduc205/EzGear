package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.AddToCartRequest;
import com.manhduc205.ezgear.dtos.responses.CartItemResponse;
import com.manhduc205.ezgear.dtos.responses.CartResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductImage;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.cart.Cart;
import com.manhduc205.ezgear.models.cart.CartItem;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
// dùng concurentHashMap an toàn hơn với đa luồng, tránh race condition
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductStockService productStockService;
    private final WarehouseService warehouseService;
    private final ProductSkuService productSkuService;
    private final CustomerAddressRepository customerAddressRepository;

    private final Map<Long, Long> warehouseCache = new ConcurrentHashMap<>();


    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder()
                                .userId(userId)
                                .items(new ArrayList<>())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));

        return buildCartResponse(cart);
    }


    // ===================== THÊM SẢN PHẨM ===================== //

    public CartResponse addItem(Long userId, AddToCartRequest req) {

        // ---- Kiểm tra tồn kho (số lượng muốn thêm) ----
        validateStock(userId, req.getSkuId(), req.getQuantity());

        // ---- Tạo giỏ nếu chưa có ----
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .userId(userId)
                        .items(new ArrayList<>())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
                ));

        // ---- Tìm xem SKU đã có trong cart chưa ----
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getSkuId().equals(req.getSkuId()))
                .findFirst()
                .orElse(null);

        int newQty = (existing != null ? existing.getQuantity() : 0) + req.getQuantity();

        // ---- Kiểm tra tồn kho tổng quantity sau khi cộng dồn ----
        validateStock(userId, req.getSkuId(), newQty);

        if (existing != null) {
            existing.setQuantity(newQty);
        } else {
            cart.getItems().add(new CartItem(req.getSkuId(), req.getQuantity(), true));
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }


    // ===================== CẬP NHẬT SỐ LƯỢNG ===================== //

    public CartResponse updateQuantity(Long userId, Long skuId, int qty) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException("Giỏ hàng rỗng"));

        validateStock(userId, skuId, qty);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getSkuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new RequestException("Sản phẩm không có trong giỏ"));

        item.setQuantity(qty);
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
        return buildCartResponse(cart);
    }


    // ===================== XOÁ SẢN PHẨM ===================== //

    public CartResponse removeItem(Long userId, Long skuId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException("Giỏ hàng rỗng"));

        boolean removed = cart.getItems().removeIf(i -> i.getSkuId().equals(skuId));

        if (!removed)
            throw new RequestException("Không tìm thấy sản phẩm cần xoá");

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }


    // ===================== XÓA TOÀN BỘ GIỎ ===================== //

    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException("Giỏ hàng rỗng"));
        cart.getItems().clear();
        cartRepository.save(cart);
    }


    // ===================== KIỂM TRA TỒN KHO ===================== //

    private void validateStock(Long userId, Long skuId, int quantity) {

        if (quantity <= 0) return;

        // Lấy warehouse từ cache hoặc tính mới
        Long warehouseId = warehouseCache.computeIfAbsent(userId, uid -> {
            var addr = customerAddressRepository.findByUserIdAndIsDefaultTrue(uid)
                    .orElseThrow(() -> new RequestException("Bạn chưa chọn địa chỉ mặc định"));
            return warehouseService.getWarehouseIdByAddress(addr);
        });

        int available = productStockService.getAvailable(skuId, warehouseId);

        if (quantity > available)
            throw new RequestException("Không đủ tồn kho. Chỉ còn " + available + " sản phẩm.");
    }


    // ===================== MAP CART → RESPONSE ===================== //

    private CartResponse buildCartResponse(Cart cart) {

        List<CartItemResponse> items = cart.getItems().stream().map(ci -> {

            ProductSKU sku = productSkuService.getById(ci.getSkuId());

            Product product = sku.getProduct();
            return CartItemResponse.builder()
                    .skuId(ci.getSkuId())
                    .productName(sku.getName())
                    .imageUrl(product.getImageUrl())
                    .price(sku.getPrice())
                    .quantity(ci.getQuantity())
                    .selected(ci.getSelected())
                    .build();

        }).toList();

        return CartResponse.builder()
                .userId(cart.getUserId())
                .items(items)
                .build();
    }
}
