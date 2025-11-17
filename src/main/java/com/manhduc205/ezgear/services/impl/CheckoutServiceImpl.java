//package com.manhduc205.ezgear.services.impl;
//
//import com.manhduc205.ezgear.dtos.request.CartItemRequest;
//import com.manhduc205.ezgear.dtos.request.CheckoutRequest;
//import com.manhduc205.ezgear.dtos.responses.CheckoutItemResponse;
//import com.manhduc205.ezgear.dtos.responses.CartCheckoutPreviewResponse;
//import com.manhduc205.ezgear.exceptions.RequestException;
//import com.manhduc205.ezgear.models.CustomerAddress;
//import com.manhduc205.ezgear.models.ProductSKU;
//import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
//import com.manhduc205.ezgear.repositories.ProductSkuRepository;
//import com.manhduc205.ezgear.services.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class CheckoutServiceImpl implements CheckoutService {
//
//    private final ProductStockService stockService;
//    private final OrderService orderService;
//    private final ProductSkuRepository productSkuRepository;
//    private final CustomerAddressRepository customerAddressRepository;
//    private final WarehouseService warehouseService;
//
//    @Transactional
//    public CartCheckoutPreviewResponse previewCheckout(CheckoutRequest req, Long userId) {
//
//        if (req.getCartItems() == null || req.getCartItems().isEmpty()) {
//            throw new RequestException("Giỏ hàng trống, không thể thanh toán.");
//        }
//
//        if (req.getAddressId() == null) {
//            throw new RequestException("Bạn chưa chọn địa chỉ giao hàng.");
//        }
//
//        CustomerAddress address = customerAddressRepository
//                .findByIdAndUserId(req.getAddressId(), userId)
//                .orElseThrow(() -> new RequestException("Địa chỉ giao hàng không hợp lệ."));
//
//        //Lấy kho theo địa chỉ
//        Long warehouseId = warehouseService.getWarehouseIdByAddress(address);
//
//        // Ktra tồn kho từng SKU
//        for (CartItemRequest item : req.getCartItems()) {
//
//            if (item.getQuantity() == null || item.getQuantity() <= 0) {
//                throw new RequestException("Số lượng không hợp lệ cho SKU " + item.getSkuId());
//            }
//
//            int available = stockService.getAvailable(item.getSkuId(), warehouseId);
//
//            if (available < item.getQuantity()) {
//                throw new RequestException(
//                        "Sản phẩm SKU " + item.getSkuId() + " không đủ tồn kho (còn " + available + ")."
//                );
//            }
//        }
//
//        //  subtotal từ giá SKU trong DB
//        Long subtotal = 0L;
//
//        List<CheckoutItemResponse> items  = new ArrayList<>();
//
//        for (CartItemRequest ci : req.getCartItems()) {
//
//            ProductSKU sku = productSkuRepository.findById(ci.getSkuId())
//                    .orElseThrow(() -> new RequestException("SKU " + ci.getSkuId() + " không tồn tại."));
//
//            Long unitPrice = sku.getPrice() != null ? sku.getPrice() : 0L;
//
//            Long lineTotal = unitPrice * ci.getQuantity();
//            subtotal += lineTotal;
//
//            items.add(
//                    CheckoutItemResponse.builder()
//                            .skuId(sku.getId())
//                            .productName(sku.getProduct().getName())
//                            .skuName(sku.getName())
//                            .quantity(ci.getQuantity())
//                            .unitPrice(unitPrice)
//                            .lineTotal(lineTotal)
//                            .build()
//            );
//        }
//
//        return CartCheckoutPreviewResponse.builder()
//                .items(items)
//                .subtotal(subtotal)
//                .discount(0L)
//                .total(subtotal)
//                .addressId(req.getAddressId())
//                .build();
//    }
//}
