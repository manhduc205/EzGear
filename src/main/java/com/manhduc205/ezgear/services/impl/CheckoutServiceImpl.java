package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.request.CheckoutRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest;
import com.manhduc205.ezgear.dtos.responses.CheckoutResponse;
import com.manhduc205.ezgear.dtos.responses.CheckoutItemPreviewResponse;
import com.manhduc205.ezgear.dtos.responses.OrderPreviewResponse;
import com.manhduc205.ezgear.dtos.responses.ShippingAddressInfo;
import com.manhduc205.ezgear.dtos.responses.VoucherInfo;
import com.manhduc205.ezgear.dtos.responses.WarehouseInfo;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.services.*;
import com.manhduc205.ezgear.shipping.dto.response.GhnShippingFeeResponse;
import com.manhduc205.ezgear.shipping.service.ShippingFeeCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final ProductStockService productStockService;
    private final WarehouseService warehouseService;
    private final ProductSkuRepository productSkuRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final ShippingFeeCalculatorService shippingFeeCalculatorService;
    private final CustomerAddressService customerAddressService;
    private final VoucherService voucherService;

    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest req, Long userId) {
        if (req.getCartItems() == null || req.getCartItems().isEmpty()) {
            throw new RequestException(Translator.toLocale("error.checkout.empty_cart"));
        }
        if (req.getAddressId() == null) {
            throw new RequestException(Translator.toLocale("error.checkout.shipping_address_required"));
        }
        if (req.getServiceId() == null) {
            throw new RequestException(Translator.toLocale("error.checkout.shipping_service_required"));
        }

        // Lấy địa chỉ & Kho
        CustomerAddress address = customerAddressRepository
                .findByIdAndUserId(req.getAddressId(), userId)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.checkout.invalid_shipping_address")));

        // Chỉ cần Tổng tồn trong Tỉnh > 0 là cho phép đặt (dù có thể phải điều chuyển kho)
        for (CartItemRequest ci : req.getCartItems()) {
            int availableInProvince = productStockService.getAvailableInProvince(ci.getSkuId(), address.getProvinceId());
            if (availableInProvince < ci.getQuantity()) {
                ProductSKU sku = productSkuRepository.findById(ci.getSkuId()).orElseThrow();
                throw new RequestException(Translator.toLocale(
                        "error.product.not_enough_stock_in_area",
                        sku.getName(),
                        availableInProvince
                ));
            }
        }

        // Gọi sang WarehouseService để tìm kho tối ưu nhất (Đủ hàng > Gần nhất)
        Warehouse hubWarehouse = warehouseService.findOptimalWarehouse(address, req.getCartItems());
        Long branchId = hubWarehouse.getBranch().getId();

        //Tính toán + Map DTO Voucher
        long itemsSubtotal = 0L;
        List<CheckoutItemPreviewResponse> itemPreviews = new ArrayList<>();
        List<CartItemRequest> cartItemsForShipping = new ArrayList<>();
        List<ApplyVoucherItemRequest> voucherItems = new ArrayList<>(); // List riêng cho Voucher

        for (CartItemRequest ci : req.getCartItems()) {
            ProductSKU sku = productSkuRepository.findById(ci.getSkuId())
                    .orElseThrow(() -> new RequestException(
                            Translator.toLocale("error.sku.not_found_by_id", ci.getSkuId())
                    ));

            if (ci.getQuantity() == null || ci.getQuantity() <= 0) {
                throw new RequestException(Translator.toLocale("error.checkout.invalid_quantity", ci.getSkuId()));
            }

            long unitPrice = sku.getPrice() != null ? sku.getPrice() : 0L;
            long lineTotal = unitPrice * ci.getQuantity();
            itemsSubtotal += lineTotal;
            Product product = sku.getProduct();

            //Preview cho fe
            CheckoutItemPreviewResponse previewItem = CheckoutItemPreviewResponse.builder()
                    .skuId(sku.getId())
                    .categoryId(product.getCategory().getId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .skuName(sku.getName())
                    .imageUrl(product.getImageUrl())
                    .price(unitPrice)
                    .quantity(ci.getQuantity())
                    .lineTotal(lineTotal)
                    .selected(true)
                    .build();
            itemPreviews.add(previewItem);

            // List cho Shipping
            CartItemRequest cartItemForShipping = new CartItemRequest();
            cartItemForShipping.setSkuId(sku.getId());
            cartItemForShipping.setQuantity(ci.getQuantity());
            cartItemsForShipping.add(cartItemForShipping);

            //  Voucher
            ApplyVoucherItemRequest voucherItem = new ApplyVoucherItemRequest();
            voucherItem.setSkuId(sku.getId());
            voucherItem.setProductId(product.getId());
            voucherItem.setCategoryId(product.getCategory().getId());
            voucherItem.setPrice(unitPrice);
            voucherItem.setQuantity(ci.getQuantity());
            voucherItems.add(voucherItem);
        }

        // Tính phí ship
        long shippingFee = 0L;
        try {
            GhnShippingFeeResponse feeRes = shippingFeeCalculatorService.calculateShippingFee(
                    branchId, address.getId(), cartItemsForShipping, req.getServiceId()
            );
            if (feeRes.getData() != null && feeRes.getData().getTotal() != null) {
                shippingFee = feeRes.getData().getTotal();
            }
        } catch (Exception e) {
            log.error("Lỗi tính phí ship checkout: {}", e.getMessage());
        }

        // Tính Voucher
        long discount = 0L;
        String voucherCode = null;

        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            voucherCode = req.getVoucherCode().trim();
            discount = voucherService.calculateDiscountForCheckout(
                    voucherCode,
                    voucherItems,
                    itemsSubtotal,
                    shippingFee
            );
        }

        long grandTotal = Math.max(0, itemsSubtotal + shippingFee - discount);

        // Build Response
        OrderPreviewResponse orderPreview = OrderPreviewResponse.builder()
                .items(itemPreviews)
                .subtotal(itemsSubtotal)
                .discount(discount)
                .shippingFee(shippingFee)
                .grandTotal(grandTotal)
                .build();

        ShippingAddressInfo addressInfo = ShippingAddressInfo.builder()
                .id(address.getId())
                .fullAddress(customerAddressService.getFullAddress(address))
                .isDefault(Boolean.TRUE.equals(address.getIsDefault()))
                .build();

        VoucherInfo voucherInfo = VoucherInfo.builder()
                .code(voucherCode)
                .discountValue(discount)
                .build();

        WarehouseInfo warehouseInfo = WarehouseInfo.builder()
                .id(hubWarehouse.getId())
                .name(hubWarehouse.getName())
                .build();

        String paymentMethod = (req.getPaymentMethod() != null)
                ? req.getPaymentMethod().toUpperCase()
                : "COD";

        return CheckoutResponse.builder()
                .orderPreview(orderPreview)
                .shippingAddress(addressInfo)
                .voucher(voucherInfo)
                .warehouse(warehouseInfo)
                .paymentMethod(paymentMethod)
                .build();
    }
}