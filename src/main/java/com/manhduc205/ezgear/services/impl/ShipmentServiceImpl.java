package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.enums.GhnRequiredNote;
import com.manhduc205.ezgear.enums.OrderStatus;
import com.manhduc205.ezgear.enums.PaymentMethod;
import com.manhduc205.ezgear.enums.ShipmentStatus;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.Shipment;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.repositories.ShipmentRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.services.ShipmentService;
import com.manhduc205.ezgear.shipping.client.GhnRestClient;
import com.manhduc205.ezgear.shipping.config.GhnProperties;
import com.manhduc205.ezgear.shipping.dto.request.GhnCreateOrderRequest;
import com.manhduc205.ezgear.shipping.dto.response.GhnCreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final OrderRepository orderRepository;
    private final WarehouseRepository warehouseRepository;
    private final ShipmentRepository shipmentRepo;
    private final ProductSkuRepository skuRepo;

    private final GhnRestClient ghnClient;
    private final GhnProperties ghnProperties;

    @Override
    public Shipment createShipment(Long orderId) {
        // lấy order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RequestException("Order not found with id: " + orderId));
        if (OrderStatus.SHIPPING.equals(order.getStatus())) {
            throw new RequestException("Đơn hàng này đã được tạo vận đơn rồi.");
        }
        // lấy hubwarehouse
        Warehouse hub = warehouseRepository.findById(order.getBranchId())
                .orElseThrow(() -> new RequestException("Warehouse not found with id: " + order.getBranchId()));
        // build request gửi GHN
        GhnCreateOrderRequest req = buildGhnRequest(order, hub);
        GhnCreateOrderResponse res = ghnClient.post("/v2/shipping-order/create", req, GhnCreateOrderResponse.class);

        if (res == null || res.getData() == null) {
            throw new RequestException("Lỗi tạo đơn GHN: " + (res != null ? res.getMessage() : "No response"));
        }
        // 5. Lưu thông tin vận chuyển vào DB
        Shipment shipment = Shipment.builder()
                .order(order)
                .provider("GHN")
                .trackingCode(res.getData().getOrderCode()) // Mã vận đơn
                .status(String.valueOf(ShipmentStatus.READY_TO_PICK)) // Trạng thái: Chờ lấy hàng
                .fee(res.getData().getTotalFee()) // Phí ship thực tế trả cho GHN
                .build();

        shipmentRepo.save(shipment);

        // 6. Cập nhật trạng thái đơn hàng
        order.setStatus("SHIPPING");
        orderRepository.save(order);

        return shipment;
    }
    private GhnCreateOrderRequest buildGhnRequest(Order order, Warehouse hubWarehouse) {
        Map<Long, ProductSKU> skuMap = skuRepo.findAllById(
                order.getItems().stream().map(i -> i.getSkuId()).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(ProductSKU::getId, s -> s));
        // Logic tính tiền thu hộ (COD Amount)
        // Nếu khách trả VNPay (PAID) thì Thu hộ = 0
        // Nếu khách chọn COD thì Thu hộ = Tổng tiền đơn
        int codAmount = 0;
        if (PaymentMethod.COD.equals(order.getPaymentMethod()) && !"PAID".equals(order.getPaymentStatus())) {
            codAmount = order.getGrandTotal().intValue();
        }
        int totalWeight = 0;
        int maxLength = 0;
        int maxWidth = 0;
        int totalHeight = 0; // Tổng chiều cao (chồng các hộp nhỏ)

        List<GhnCreateOrderRequest.Item> items = new ArrayList<>();

        for (var orderItem : order.getItems()) {
            ProductSKU sku = skuMap.get(orderItem.getSkuId());
            if (sku == null) continue;

            int quantity = orderItem.getQuantity();
            int w = sku.getWeightGram() != null ? sku.getWeightGram() : 200;
            int l = sku.getLengthCm() != null ? sku.getLengthCm() : 10;
            int wd = sku.getWidthCm() != null ? sku.getWidthCm() : 10;
            int h = sku.getHeightCm() != null ? sku.getHeightCm() : 10;

            totalWeight += w * quantity;
            maxLength = Math.max(maxLength, l); // Lấy kích thước cạnh dài nhất
            maxWidth = Math.max(maxWidth, wd);
            totalHeight += h * quantity; // Cộng dồn chiều cao

            items.add(GhnCreateOrderRequest.Item.builder()
                    .name(orderItem.getProductNameSnapshot())
                    .code(orderItem.getSkuId().toString())
                    .quantity(quantity)
                    .price(orderItem.getUnitPrice().intValue())
                    .weight(w)
                    .build());
        }
        // int serviceId = order.getShippingServiceId() != null ? order.getShippingServiceId() : 53320;
        int serviceId = 53320;

        // D. Build Request
        return GhnCreateOrderRequest.builder()
                .paymentTypeId(2) // người mua trả ship
                .note(order.getNote())
                .requiredNote(String.valueOf(GhnRequiredNote.CHOXEMHANGKHONGTHU))
                .toName(order.getShippingAddress().getReceiverName())
                .toPhone(order.getShippingAddress().getReceiverPhone())
                .toAddress(order.getShippingAddress().getAddressLine())
                .toWardCode(order.getShippingAddress().getWardCode())
                .toDistrictId(order.getShippingAddress().getDistrictId())

                .codAmount(codAmount)
                .insuranceValue(Math.min(order.getGrandTotal().intValue(), 5000000))
                .serviceId(serviceId)

                // Kích thước động
                .weight(totalWeight)
                .length(maxLength)
                .width(maxWidth)
                .height(totalHeight)

                .items(items)
                .shopId(Integer.parseInt(ghnProperties.getActiveShopId()))
                .build();
    }

}
