package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.request.CheckoutRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherItemRequest;
import com.manhduc205.ezgear.dtos.request.voucher.ApplyVoucherRequest;
import com.manhduc205.ezgear.dtos.responses.*;
import com.manhduc205.ezgear.dtos.responses.voucher.ApplyVoucherResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.services.*;
import com.manhduc205.ezgear.shipping.dto.response.GhnShippingFeeResponse;
import com.manhduc205.ezgear.shipping.service.ShippingFeeCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
            throw new RequestException("Giỏ hàng trống, không thể thanh toán.");
        }

        if (req.getAddressId() == null) {
            throw new RequestException("Bạn chưa chọn địa chỉ giao hàng.");
        }

        if (req.getServiceId() == null) {
            throw new RequestException("Bạn chưa chọn phương thức vận chuyển.");
        }

        //Lấy địa chỉ
        CustomerAddress address = customerAddressRepository
                .findByIdAndUserId(req.getAddressId(), userId)
                .orElseThrow(() -> new RequestException("Địa chỉ giao hàng không hợp lệ."));

        // lấy kho phù hợp từ địa chỉ
        Warehouse warehouse = warehouseService.resolveWarehouseForAddress(address);
        Long warehouseId = warehouse.getId();
        Long branchId = (warehouse.getBranch() != null) ? warehouse.getBranch().getId() : null;

        if (branchId == null) {
            throw new RequestException("Kho không gắn với chi nhánh hợp lệ.");
        }

        //Kiểm tra tồn kho theo kho
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

        // Tính subtotal + build orderItems + item preview
        long itemsSubtotal = 0L;
        List<CheckoutItemPreviewResponse> itemPreviews = new ArrayList<>();

        for (CartItemRequest ci : req.getCartItems()) {
            ProductSKU sku = productSkuRepository.findById(ci.getSkuId())
                    .orElseThrow(() -> new RequestException("SKU " + ci.getSkuId() + " không tồn tại."));

            if (ci.getQuantity() == null || ci.getQuantity() <= 0) {
                throw new RequestException("Số lượng không hợp lệ cho SKU " + ci.getSkuId());
            }

            Long unitPrice = sku.getPrice() != null ? sku.getPrice() : 0L;
            long lineTotal = unitPrice * ci.getQuantity();
            itemsSubtotal += lineTotal;

            Product product = sku.getProduct();

            CheckoutItemPreviewResponse previewItem = CheckoutItemPreviewResponse.builder()
                    .skuId(sku.getId())
                    .categoryId(product.getCategory().getId())
                    .productId(product.getId())
                    .productName(product != null ? product.getName() : null)
                    .skuName(sku.getName())
                    .imageUrl(product != null ? product.getImageUrl() : null)
                    .price(unitPrice)
                    .quantity(ci.getQuantity())
                    .lineTotal(lineTotal)
                    .selected(true)
                    .build();
            itemPreviews.add(previewItem);
        }

        // Tính phí ship theo GHN dùng branchId nơi gửi hàng
        Long firstSkuId = req.getCartItems().get(0).getSkuId();
        GhnShippingFeeResponse feeRes = shippingFeeCalculatorService
                .calculateShippingFee(branchId, address.getId(), firstSkuId, req.getServiceId());

        long shippingFee = 0L;
        if (feeRes.getData() != null && feeRes.getData().getTotal() != null) {
            shippingFee = feeRes.getData().getTotal();
        }

        // Voucher (tạm hardcode)
        // Voucher: BE tự tính lại để tránh gian lận
        // =======================
//  VOUCHER (backend tự tính, không tin FE)
// =======================
        long discount = 0L;
        String voucherCode = null;

        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            voucherCode = req.getVoucherCode().trim();

            // Build request cho VoucherService dựa trên dữ liệu checkout hiện tại
            ApplyVoucherRequest voucherReq = new ApplyVoucherRequest();
            voucherReq.setCode(voucherCode);
            voucherReq.setSubtotal(itemsSubtotal);
            voucherReq.setShippingFee(shippingFee);

            List<ApplyVoucherItemRequest> voucherItems = new ArrayList<>();
            for (CheckoutItemPreviewResponse preview : itemPreviews) {
                ApplyVoucherItemRequest itemReq = new ApplyVoucherItemRequest();
                itemReq.setSkuId(preview.getSkuId());
                itemReq.setProductId(preview.getProductId());
                itemReq.setCategoryId(preview.getCategoryId());
                itemReq.setPrice(preview.getPrice());
                itemReq.setQuantity(preview.getQuantity());
                voucherItems.add(itemReq);
            }
            voucherReq.setItems(voucherItems);

            // Gọi service voucher dùng lại logic hiện có
            ApplyVoucherResponse voucherRes = voucherService.applyVoucher(voucherReq);
            discount = voucherRes.getDiscount();

            // Safety nhỏ cho chắc
            if (discount < 0) discount = 0;
            if (discount > itemsSubtotal + shippingFee) {
                discount = itemsSubtotal + shippingFee;
            }
        }


        long grandTotal = itemsSubtotal + shippingFee - discount;
        if (grandTotal < 0) grandTotal = 0;

        // Build các block thông tin trả về cho FE
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
                .id(warehouseId)
                .name(warehouse.getName())
                .build();

        String paymentMethod = (req.getPaymentMethod() != null)
                ? req.getPaymentMethod().toUpperCase()
                : "COD";

        //preview
        return CheckoutResponse.builder()
                .orderPreview(orderPreview)
                .shippingAddress(addressInfo)
                .voucher(voucherInfo)
                .warehouse(warehouseInfo)
                .paymentMethod(paymentMethod)
                .build();
    }

}
