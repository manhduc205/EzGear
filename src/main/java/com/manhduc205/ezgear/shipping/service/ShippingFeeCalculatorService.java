package com.manhduc205.ezgear.shipping.service;

import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.shipping.client.GhnRestClient;
import com.manhduc205.ezgear.shipping.config.GhnProperties;
import com.manhduc205.ezgear.shipping.dto.request.GhnAvailableServiceRequest;
import com.manhduc205.ezgear.shipping.dto.request.GhnShippingFeeRequest;
import com.manhduc205.ezgear.shipping.dto.response.AvailableServiceResponse;
import com.manhduc205.ezgear.shipping.dto.response.GhnAvailableServiceResponse;
import com.manhduc205.ezgear.shipping.dto.response.GhnShippingFeeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingFeeCalculatorService {

    private final BranchRepository branchRepo;
    private final CustomerAddressRepository addressRepo;
    private final ProductSkuRepository skuRepo;
    private final LocationRepository locationRepo;
    private final GhnRestClient ghnClient;
    private final GhnAvailableService ghnAvailableService;
    private final GhnProperties ghnProperties;

    /**
     * Tính phí GHN dựa trên:
     * - Chi nhánh gửi hàng (Branch)
     * - Địa chỉ nhận hàng (CustomerAddress)
     * - Thông tin sản phẩm (ProductSKU)
     */
    public AvailableServiceResponse getAvailableServices(Long branchId, Long addressId) {

        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        CustomerAddress address = addressRepo.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        Location fromDistrict = findParentDistrict(branch.getLocation());
        Location toDistrict = findParentDistrict(address.getLocation());

        if (fromDistrict == null || toDistrict == null) {
            throw new RuntimeException("District hierarchy invalid.");
        }

        // Gọi GHN available service API
        GhnAvailableServiceRequest req = new GhnAvailableServiceRequest();
        req.setFromDistrict(Integer.parseInt(fromDistrict.getGhnCode()));
        req.setToDistrict(Integer.parseInt(toDistrict.getGhnCode()));
        req.setShopId(Integer.parseInt(ghnProperties.getActiveShopId()));

        GhnAvailableServiceResponse res = ghnAvailableService.getAvailableServices(req);

        if (res == null || res.getData() == null || res.getData().isEmpty()) {
            throw new RuntimeException("No GHN services found.");
        }

        // default nhanh, nếu không có thì lấy cái đầu
        Integer defaultServiceId = res.getData().stream()
                .filter(s -> "Nhanh".equalsIgnoreCase(s.getShortName()))
                .map(GhnAvailableServiceResponse.ServiceData::getServiceId)
                .findFirst()
                .orElse(res.getData().get(0).getServiceId());

        return AvailableServiceResponse.builder()
                .defaultServiceId(defaultServiceId)
                .services(res.getData())
                .build();
    }

    public GhnShippingFeeResponse calculateShippingFee(Long branchId, Long addressId, Long skuId, Integer serviceId) {
        if (serviceId == null) {
            throw new RuntimeException("serviceId is required.");
        }
        // Lấy chi nhánh gửi hàng
        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        Location branchLocation = branch.getLocation();

        // Lấy địa chỉ nhận hàng của khách
        CustomerAddress address = addressRepo.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Customer address not found"));
        Location addressLocation = address.getLocation();

        // Tìm quận/huyện và phường của 2 bên
        Location fromDistrict = findParentDistrict(branchLocation);
        Location toDistrict = findParentDistrict(addressLocation);
        Location toWard = findWard(addressLocation);

        if (fromDistrict == null || toDistrict == null || toWard == null) {
            throw new RuntimeException("Invaliid GHN location hierarchy: missng district or ward");
        }

        // Lấy thông tin sản phẩm
        ProductSKU sku = skuRepo.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Product SKU not found"));

        int weight = sku.getWeightGram() != null ? sku.getWeightGram() : 200;
        int length = sku.getLengthCm() != null ? sku.getLengthCm() : 10;
        int width  = sku.getWidthCm()  != null ? sku.getWidthCm()  : 10;
        int height = sku.getHeightCm() != null ? sku.getHeightCm() : 10;

//        GhnAvailableServiceRequest serviceReq = new GhnAvailableServiceRequest();
//        serviceReq.setFromDistrict(Integer.parseInt(fromDistrict.getGhnCode()));
//        serviceReq.setToDistrict(Integer.parseInt(toDistrict.getGhnCode()));
//        // shop id
//        serviceReq.setShopId(Integer.parseInt(ghnProperties.getActiveShopId()));
//
//        GhnAvailableServiceResponse serviceRes =
//                ghnAvailableService.getAvailableServices(serviceReq);

//        if (serviceRes == null || serviceRes.getData() == null || serviceRes.getData().isEmpty()) {
//            throw new RuntimeException("No available GHN services found");
//        }

        // - Chuẩn bị request gửi GHN
        GhnShippingFeeRequest req = new GhnShippingFeeRequest();
        req.setServiceId(serviceId);
        req.setInsuranceValue(Math.min(sku.getPrice().intValue(), 5_000_000));
        req.setFromDistrictId(Integer.parseInt(fromDistrict.getGhnCode()));
        req.setToDistrictId(Integer.parseInt(toDistrict.getGhnCode()));
        req.setToWardCode(toWard.getGhnCode());
        req.setWeight(weight);
        req.setLength(length);
        req.setWidth(width);
        req.setHeight(height);

        log.info(" [GHN Fee Request] {}", req);

        // Gọi API GHN
        GhnShippingFeeResponse response =
                ghnClient.post("/v2/shipping-order/fee", req, GhnShippingFeeResponse.class);

        if (response == null || response.getData() == null) {
            throw new RuntimeException("Failed to calculate GHN shipping fee");
        }

        return response;
    }


    /** Tìm cấp quận/huyện của một Location */
    private Location findParentDistrict(Location loc) {
        if (loc == null) return null;
        if (loc.getLevel() == Location.Level.DISTRICT) return loc;

        Location parent = loc.getParent();
        if (parent == null) return null;

        return findParentDistrict(parent);
    }

    private Location findWard(Location loc) {
        if (loc == null) return null;
        if (loc.getLevel() == Location.Level.WARD) return loc;
        return null; // GHN yêu cầu đúng cấp phường, không suy ngược xuống được
    }
}
