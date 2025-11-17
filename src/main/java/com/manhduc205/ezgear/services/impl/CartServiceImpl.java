package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.AddToCartRequest;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.request.CartCheckoutRequest;
import com.manhduc205.ezgear.dtos.responses.CartItemResponse;
import com.manhduc205.ezgear.dtos.responses.CartResponse;
import com.manhduc205.ezgear.dtos.responses.CheckoutItemResponse;
import com.manhduc205.ezgear.dtos.responses.CartCheckoutPreviewResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.cart.Cart;
import com.manhduc205.ezgear.models.cart.CartItem;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.repositories.cart.CartRepository;
import com.manhduc205.ezgear.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
// dùng concurentHashMap an toàn hơn với đa luồng, tránh race condition
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductStockService productStockService;
    private final WarehouseService warehouseService;
    private final ProductSkuService productSkuService;
    private final CustomerAddressRepository customerAddressRepository;

    private final Map<Long, Long> warehouseCache = new ConcurrentHashMap<>();
    private final ProductSkuRepository productSkuRepository;

    @Transactional
    public CartCheckoutPreviewResponse previewCheckout(CartCheckoutRequest req, Long userId) {

        if (req.getCartItems() == null || req.getCartItems().isEmpty()) {
            throw new RequestException("Giỏ hàng trống, không thể thanh toán.");
        }


        CustomerAddress address = customerAddressRepository
                .findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new RequestException("Bạn chưa thiết lập địa chỉ mặc định"));

        //Lấy kho theo địa chỉ
        Long warehouseId = warehouseService.getWarehouseIdByAddress(address);

        // Ktra tồn kho từng SKU
        for (CartItemRequest item : req.getCartItems()) {

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RequestException("Số lượng không hợp lệ cho SKU " + item.getSkuId());
            }

            int available = productStockService.getAvailable(item.getSkuId(), warehouseId);

            if (available < item.getQuantity()) {
                throw new RequestException(
                        "Sản phẩm SKU " + item.getSkuId() + " không đủ tồn kho (còn " + available + ")."
                );
            }
        }

        //  subtotal từ giá SKU trong DB
        Long subtotal = 0L;

        List<CheckoutItemResponse> items  = new ArrayList<>();

        for (CartItemRequest ci : req.getCartItems()) {

            ProductSKU sku = productSkuRepository.findById(ci.getSkuId())
                    .orElseThrow(() -> new RequestException("SKU " + ci.getSkuId() + " không tồn tại."));

            Long unitPrice = sku.getPrice() != null ? sku.getPrice() : 0L;

            Long lineTotal = unitPrice * ci.getQuantity();
            subtotal += lineTotal;

            items.add(
                    CheckoutItemResponse.builder()
                            .skuId(sku.getId())
                            .productName(sku.getProduct().getName())
                            .skuName(sku.getName())
                            .quantity(ci.getQuantity())
                            .unitPrice(unitPrice)
                            .lineTotal(lineTotal)
                            .build()
            );
        }

        return CartCheckoutPreviewResponse.builder()
                .items(items)
                .subtotal(subtotal)
                .total(subtotal)
                .build();
    }

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


    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException("Giỏ hàng rỗng"));
        cart.getItems().clear();
        cartRepository.save(cart);
    }


    //ktra tồn kho

    private void validateStock(Long userId, Long skuId, int quantity) {

        if (quantity <= 0) return;

        CustomerAddress addr = customerAddressRepository
                .findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new RequestException("Bạn chưa thiết lập địa chỉ mặc định"));

        Long warehouseId = warehouseService.getWarehouseIdByAddress(addr);

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
