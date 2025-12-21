package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.request.CartCheckoutRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest;
import com.manhduc205.ezgear.dtos.responses.CartItemResponse;
import com.manhduc205.ezgear.dtos.responses.CartResponse;
import com.manhduc205.ezgear.dtos.responses.CheckoutItemResponse;
import com.manhduc205.ezgear.dtos.responses.CartCheckoutPreviewResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.models.cart.Cart;
import com.manhduc205.ezgear.models.cart.CartItem;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.repositories.cart.CartRepository;
import com.manhduc205.ezgear.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductStockService productStockService;
    private final WarehouseService warehouseService;
    private final ProductSkuService productSkuService;
    private final CustomerAddressRepository customerAddressRepository;
    private final ProductSkuRepository productSkuRepository;
    private final VoucherService voucherService;
    private final WarehouseRepository warehouseRepository;

    // ======================= PREVIEW CHECKOUT (XEM TRƯỚC) ======================= //

    @Override
    @Transactional(readOnly = true)
    public CartCheckoutPreviewResponse previewCheckout(CartCheckoutRequest req, Long userId) {

        if (req.getCartItems() == null || req.getCartItems().isEmpty()) {
            throw new RequestException(Translator.toLocale("error.cart.empty"));
        }

        // 1. Tìm kho dựa trên Tỉnh/Thành phố (Location Context)
        // Không cần địa chỉ cụ thể, chỉ cần ProvinceId để biết kho nào phục vụ khu vực này
        Long warehouseId = resolveWarehouseByLocation(req.getProvinceId(), req.getCartItems());

        // 2. Tính toán Subtotal và Build Items
        long subtotal = 0L;
        List<CheckoutItemResponse> items = new ArrayList<>();
        List<ApplyVoucherItemRequest> voucherItems = new ArrayList<>();

        for (CartItemRequest ci : req.getCartItems()) {
            // Check tồn kho tại kho khu vực này
            int available = productStockService.getAvailable(ci.getSkuId(), warehouseId);
            if (available < ci.getQuantity()) {
                throw new RequestException(Translator.toLocale(
                        "error.cart.sku_out_of_stock_in_area",
                        ci.getSkuId()
                ));
            }

            ProductSKU sku = productSkuRepository.findById(ci.getSkuId())
                    .orElseThrow(() -> new RequestException(
                            Translator.toLocale("error.cart.sku_not_found", ci.getSkuId())
                    ));

            Long unitPrice = sku.getPrice() != null ? sku.getPrice() : 0L;
            long lineTotal = unitPrice * ci.getQuantity();
            subtotal += lineTotal;

            Product product = sku.getProduct();

            items.add(CheckoutItemResponse.builder()
                    .skuId(sku.getId())
                    .productName(product != null ? product.getName() : null)
                    .skuName(sku.getName())
                    .quantity(ci.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build());

            // Map sang DTO Voucher
            ApplyVoucherItemRequest vItem = new ApplyVoucherItemRequest();
            vItem.setSkuId(sku.getId());
            vItem.setProductId(sku.getProduct().getId());
            if (sku.getProduct().getCategory() != null) {
                vItem.setCategoryId(sku.getProduct().getCategory().getId());
            }
            vItem.setPrice(unitPrice);
            vItem.setQuantity(ci.getQuantity());
            voucherItems.add(vItem);
        }

        // 3. Tính Voucher (Phí ship mặc định = 0)
        long discount = 0L;
        long shippingFee = 0L;

        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            discount = voucherService.calculateDiscountForCheckout(
                    req.getVoucherCode(),
                    voucherItems,
                    subtotal,
                    shippingFee
            );
        }

        long total = Math.max(0, subtotal - discount);

        return CartCheckoutPreviewResponse.builder()
                .items(items)
                .subtotal(subtotal)
                .discount(discount)
                .total(total)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId, Integer provinceId) {
        Cart cart = getOrCreateCart(userId);

        // Mặc định nếu không có provinceId thì lấy Hà Nội (201) hoặc 0 (check toàn quốc)
        int currentProvinceId = (provinceId != null) ? provinceId : 201;

        List<CartItemResponse> items = cart.getItems().stream().map(ci -> {
            ProductSKU sku = productSkuService.getById(ci.getSkuId());

            // CHECK TỒN KHO THEO TỈNH
            int available = productStockService.getAvailableInProvince(ci.getSkuId(), currentProvinceId);
            // Nếu tồn kho = 0 -> Chắc chắn hết
            // Nếu khách mua 5 mà kho còn 2
            boolean isOOS = (available < ci.getQuantity());

            return CartItemResponse.builder()
                    .skuId(ci.getSkuId())
                    .skuName(sku.getName())
                    .productName(sku.getProduct().getName())
                    .skuName(sku.getName())
                    .imageUrl(sku.getProduct().getImageUrl())
                    .price(sku.getPrice())
                    .quantity(ci.getQuantity())
                    .selected(ci.getSelected())

                    // Gán giá trị
                    .isOutOfStock(isOOS)
                    .availableQuantity(available)
                    .build();
        }).toList();

        return CartResponse.builder().userId(userId).items(items).build();
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest req, Integer provinceId) {
        int currentProvinceId = (provinceId != null) ? provinceId : 201;

        // Validate theo Tỉnh đang chọn (Không bắt địa chỉ mặc định nữa)
        validateStockByProvince(req.getSkuId(), req.getQuantity(), currentProvinceId);

        Cart cart = getOrCreateCart(userId);
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getSkuId().equals(req.getSkuId()))
                .findFirst().orElse(null);

        int newQty = (existing != null ? existing.getQuantity() : 0) + req.getQuantity();

        // Check lại tổng số lượng
        validateStockByProvince(req.getSkuId(), newQty, currentProvinceId);

        if (existing != null) {
            existing.setQuantity(newQty);
        } else {
            cart.getItems().add(new CartItem(req.getSkuId(), req.getQuantity(), true));
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return buildCartResponse(cart, currentProvinceId);
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(Long userId, Long skuId, int qty, Integer provinceId) {
        int currentProvinceId = (provinceId != null) ? provinceId : 201;

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.cart.empty")));

        validateStockByProvince(skuId, qty, currentProvinceId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getSkuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.cart.item_not_found")));

        item.setQuantity(qty);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return buildCartResponse(cart, currentProvinceId);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long skuId, Integer provinceId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.cart.empty")));

        boolean removed = cart.getItems().removeIf(i -> i.getSkuId().equals(skuId));
        if (!removed) {
            throw new RequestException(Translator.toLocale("error.cart.item_not_found"));
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        // Mặc định Hà Nội nếu null (để tránh lỗi hiển thị)
        int currentProvinceId = (provinceId != null) ? provinceId : 201;

        return buildCartResponse(cart, currentProvinceId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.cart.empty")));
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder()
                                .userId(userId)
                                .items(new ArrayList<>())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));
    }

    // Logic tìm kho theo Tỉnh/Thành
    private Long resolveWarehouseByLocation(Integer provinceId, List<CartItemRequest> items) {
        // Chỉ lấy danh sách kho trong Tỉnh đang chọn
        List<Warehouse> warehouses = warehouseRepository.findActiveWarehousesByProvince(provinceId);

        if (warehouses.isEmpty()) {
            throw new RequestException(Translator.toLocale("error.cart.no_branch_in_area"));
        }

        // Tìm kho nào trong tỉnh đó có đủ hàng
        for (Warehouse wh : warehouses) {
            boolean isEnough = true;
            for (CartItemRequest item : items) {
                int available = productStockService.getAvailable(item.getSkuId(), wh.getId());
                if (available < item.getQuantity()) {
                    isEnough = false;
                    break;
                }
            }
            if (isEnough) return wh.getId(); // Tìm thấy kho cùng tỉnh có hàng
        }

        throw new RequestException(Translator.toLocale("error.cart.out_of_stock_in_area"));
    }

    private void validateStockByProvince(Long skuId, int quantity, Integer provinceId) {
        if (quantity <= 0) return;

        // Gọi ProductStockService để lấy tổng tồn trong tỉnh
        int availableInProvince = productStockService.getAvailableInProvince(skuId, provinceId);

        if (quantity > availableInProvince) {
            throw new RequestException(Translator.toLocale("error.cart.sku_out_of_stock_in_area", skuId));
        }
    }

    private CartResponse buildCartResponse(Cart cart, int provinceId) {
        List<CartItemResponse> items = cart.getItems().stream().map(ci -> {
            ProductSKU sku = productSkuService.getById(ci.getSkuId());

            // Check tồn kho realtime theo tỉnh
            int available = productStockService.getAvailableInProvince(ci.getSkuId(), provinceId);
            boolean isOOS = (available < ci.getQuantity());

            return CartItemResponse.builder()
                    .skuId(ci.getSkuId())
                    .skuName(sku.getName())
                    .productName(sku.getProduct().getName())
                    .imageUrl(sku.getProduct().getImageUrl())
                    .price(sku.getPrice())
                    .quantity(ci.getQuantity())
                    .selected(ci.getSelected())
                    .isOutOfStock(isOOS)
                    .availableQuantity(available)
                    .build();
        }).toList();

        return CartResponse.builder()
                .userId(cart.getUserId())
                .items(items)
                .build();
    }
}