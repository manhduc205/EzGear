package com.manhduc205.ezgear.shipping.service;

import com.manhduc205.ezgear.models.Branch;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.repositories.BranchRepository;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
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
    private final GhnRestClient ghnClient;
    private final GhnAvailableService ghnAvailableService;
    private final GhnProperties ghnProperties;

    /**
     * Lấy danh sách dịch vụ GHN khả dụng cho:
     *  - Chi nhánh gửi (branchId)
     *  - Địa chỉ nhận (addressId)
     */
    public AvailableServiceResponse getAvailableServices(Long branchId, Long addressId) {
        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        CustomerAddress address = addressRepo.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        Integer fromDistrict = branch.getDistrictId();
        Integer toDistrict = address.getDistrictId();

        if (fromDistrict == null || toDistrict == null) {
            throw new RuntimeException("Thiếu districtId của branch hoặc address.");
        }
        GhnAvailableServiceRequest req = new GhnAvailableServiceRequest();
        req.setFromDistrict(fromDistrict);
        req.setToDistrict(toDistrict);
        req.setShopId(Integer.parseInt(ghnProperties.getActiveShopId()));

        GhnAvailableServiceResponse res = ghnAvailableService.getAvailableServices(req);

        if (res == null || res.getData() == null || res.getData().isEmpty()) {
            throw new RuntimeException("No GHN services found.");
        }

        // Ưu tiên dịch vụ "nhanh", nếu không có thì lấy cái đầu
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

    /**
     * Tính phí GHN với:
     *  - branchId: chi nhánh gửi hàng
     *  - addressId: địa chỉ nhận hàng
     *  - skuId: SKU dùng để lấy weight & kích thước
     *  - serviceId: dịch vụ GHN mà FE chọn
     */
    public GhnShippingFeeResponse calculateShippingFee(Long branchId, Long addressId,
                                                       Long skuId, Integer serviceId) {
        if (serviceId == null) {
            throw new RuntimeException("serviceId is required.");
        }

        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        CustomerAddress address = addressRepo.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Customer address not found"));

        Integer fromDistrict = branch.getDistrictId();
        Integer toDistrict = address.getDistrictId();
        String toWardCode = address.getWardCode();

        if (fromDistrict == null || toDistrict == null || toWardCode == null) {
            throw new RuntimeException("Thiếu thông tin GHN (district / ward) cho branch hoặc address.");
        }

        ProductSKU sku = skuRepo.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Product SKU not found"));

        int weight = sku.getWeightGram() != null ? sku.getWeightGram() : 200;
        int length = sku.getLengthCm() != null ? sku.getLengthCm() : 10;
        int width  = sku.getWidthCm()  != null ? sku.getWidthCm()  : 10;
        int height = sku.getHeightCm() != null ? sku.getHeightCm() : 10;

        GhnShippingFeeRequest req = new GhnShippingFeeRequest();
        req.setServiceId(serviceId);
        req.setInsuranceValue(Math.min(sku.getPrice().intValue(), 5_000_000));
        req.setFromDistrictId(fromDistrict);
        req.setToDistrictId(toDistrict);
        req.setToWardCode(toWardCode);
        req.setWeight(weight);
        req.setLength(length);
        req.setWidth(width);
        req.setHeight(height);

        log.info("[GHN Fee Request] {}", req);

        GhnShippingFeeResponse response =
                ghnClient.post("/v2/shipping-order/fee", req, GhnShippingFeeResponse.class);

        if (response == null || response.getData() == null) {
            throw new RuntimeException("Failed to calculate GHN shipping fee");
        }

        return response;
    }
}
