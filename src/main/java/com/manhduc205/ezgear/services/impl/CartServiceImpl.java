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
import com.manhduc205.ezgear.services.CartService;
import com.manhduc205.ezgear.services.ProductSkuService;
import com.manhduc205.ezgear.services.ProductStockService;
import com.manhduc205.ezgear.services.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductStockService productStockService;
    private final WarehouseService warehouseService;
    private final ProductSkuService productSkuService;
    private final CustomerAddressRepository customerAddressRepository;
    private final ProductSkuRepository productSkuRepository;

    // Cache warehouseId theo addressId để đỡ gọi lại logic chọn kho nhiều lần
    private final Map<Long, Long> warehouseCache = new ConcurrentHashMap<>();

    // ======================= PREVIEW CHECKOUT ======================= //

    @Override
    @Transactional(readOnly = true)
    public CartCheckoutPreviewResponse previewCheckout(CartCheckoutRequest req, Long userId) {

        if (req.getCartItems() == null || req.getCartItems().isEmpty()) {
            throw new RequestException("Giỏ hàng trống, không thể thanh toán.");
        }

        // Lấy địa chỉ mặc định của user
        CustomerAddress address = customerAddressRepository
                .findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new RequestException("Bạn chưa thiết lập địa chỉ mặc định."));

        // Lấy warehouseId theo địa chỉ (có cache)
        Long warehouseId = resolveWarehouseId(address);

        // Kiểm tra tồn kho từng SKU
        for (CartItemRequest item : req.getCartItems()) {

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RequestException("Số lượng không hợp lệ cho SKU " + item.getSkuId());
            }

            int available = productStockService.getAvailable(item.getSkuId(), warehouseId);

            if (available < item.getQuantity()) {
                throw new RequestException(
                        "Sản phẩm SKU " + item.getSkuId()
                                + " không đủ tồn kho (còn " + available + ")."
                );
            }
        }

        // Tính subtotal từ giá SKU trong DB
        long subtotal = 0L;
        List<CheckoutItemResponse> items = new ArrayList<>();

        for (CartItemRequest ci : req.getCartItems()) {

            ProductSKU sku = productSkuRepository.findById(ci.getSkuId())
                    .orElseThrow(() -> new RequestException("SKU " + ci.getSkuId() + " không tồn tại."));

            Long unitPrice = sku.getPrice() != null ? sku.getPrice() : 0L;
            long lineTotal = unitPrice * ci.getQuantity();
            subtotal += lineTotal;

            Product product = sku.getProduct();

            items.add(
                    CheckoutItemResponse.builder()
                            .skuId(sku.getId())
                            .productName(product != null ? product.getName() : null)
                            .skuName(sku.getName())
                            .quantity(ci.getQuantity())
                            .unitPrice(unitPrice)
                            .lineTotal(lineTotal)
                            .build()
            );
        }

        // Tạm: voucher giảm cố định 10k nếu có nhập
        long discount = 0L;
        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            discount = 10_000L;
            if (discount > subtotal) {
                discount = subtotal;
            }
        }

        long total = subtotal - discount;
        if (total < 0) total = 0L;

        return CartCheckoutPreviewResponse.builder()
                .items(items)
                .subtotal(subtotal)
                .discount(discount)
                .total(total)
                .build();
    }

    // ======================= CRUD GIỎ HÀNG ======================= //

    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest req) {

        // Kiểm tra tồn kho theo số lượng muốn thêm
        validateStock(userId, req.getSkuId(), req.getQuantity());

        // Tạo giỏ nếu chưa có
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder()
                                .userId(userId)
                                .items(new ArrayList<>())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));

        // Tìm SKU đã có trong cart chưa
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getSkuId().equals(req.getSkuId()))
                .findFirst()
                .orElse(null);

        int newQty = (existing != null ? existing.getQuantity() : 0) + req.getQuantity();

        // Kiểm tra tồn kho với tổng quantity mới
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

    @Override
    @Transactional
    public CartResponse updateQuantity(Long userId, Long skuId, int qty) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException("Giỏ hàng rỗng"));

        // Kiểm tra tồn kho với số lượng mới
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

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long skuId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException("Giỏ hàng rỗng"));

        boolean removed = cart.getItems().removeIf(i -> i.getSkuId().equals(skuId));

        if (!removed) {
            throw new RequestException("Không tìm thấy sản phẩm cần xoá.");
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException("Giỏ hàng rỗng"));

        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    // ===================== KIỂM TRA TỒN KHO ===================== //

    private void validateStock(Long userId, Long skuId, int quantity) {

        if (quantity <= 0) return;

        CustomerAddress addr = customerAddressRepository
                .findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new RequestException("Bạn chưa thiết lập địa chỉ mặc định."));

        Long warehouseId = resolveWarehouseId(addr);

        int available = productStockService.getAvailable(skuId, warehouseId);

        if (quantity > available) {
            throw new RequestException("Không đủ tồn kho. Chỉ còn " + available + " sản phẩm.");
        }
    }

    // Cache warehouseId theo addressId để đỡ tính lại nhiều lần
    private Long resolveWarehouseId(CustomerAddress address) {
        if (address == null || address.getId() == null) {
            throw new RequestException("Địa chỉ giao hàng không hợp lệ.");
        }

        Long addressId = address.getId();

        return warehouseCache.computeIfAbsent(addressId, id ->
                warehouseService.getWarehouseIdByAddress(address)
        );
    }

    // ===================== MAP CART → RESPONSE ===================== //

    private CartResponse buildCartResponse(Cart cart) {

        List<CartItemResponse> items = cart.getItems().stream().map(ci -> {

            ProductSKU sku = productSkuService.getById(ci.getSkuId());
            Product product = sku.getProduct();

            return CartItemResponse.builder()
                    .skuId(ci.getSkuId())
                    .productName(product != null ? product.getName() : sku.getName())
                    .imageUrl(product != null ? product.getImageUrl() : null)
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
