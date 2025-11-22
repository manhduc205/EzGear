package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.CustomerAddressDTO;
import com.manhduc205.ezgear.dtos.request.CustomerAddressRequest;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.shipping.service.GhnMasterDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final GhnMasterDataService ghnMasterDataService;


    public CustomerAddressDTO createAddress(CustomerAddressRequest req) {

        if (req.getProvinceId() == null || req.getDistrictId() == null || req.getWardCode() == null) {
            throw new RuntimeException("Thiếu thông tin tỉnh / quận / phường");
        }

        CustomerAddress address = CustomerAddress.builder()
                .userId(req.getUserId())
                .receiverName(req.getReceiverName())
                .receiverPhone(req.getReceiverPhone())
                .provinceId(req.getProvinceId())
                .districtId(req.getDistrictId())
                .wardCode(req.getWardCode())
                .addressLine(req.getAddressLine())
                .label(req.getLabel())
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .build();

        // Clear default cũ nếu chọn default
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            customerAddressRepository.clearDefaultForUser(req.getUserId());
        }

        CustomerAddress saved = customerAddressRepository.save(address);
        return toDto(saved);
    }

    @Transactional
    public CustomerAddressDTO updateAddress(Long id, CustomerAddressRequest req) {

        CustomerAddress address = customerAddressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer address not found"));

        if (req.getReceiverName() != null)
            address.setReceiverName(req.getReceiverName());

        if (req.getReceiverPhone() != null)
            address.setReceiverPhone(req.getReceiverPhone());

        if (req.getProvinceId() != null)
            address.setProvinceId(req.getProvinceId());

        if (req.getDistrictId() != null)
            address.setDistrictId(req.getDistrictId());

        if (req.getWardCode() != null)
            address.setWardCode(req.getWardCode());

        if (req.getAddressLine() != null)
            address.setAddressLine(req.getAddressLine());

        if (req.getLabel() != null)
            address.setLabel(req.getLabel());

        if (req.getIsDefault() != null) {
            if (req.getIsDefault()) {
                customerAddressRepository.clearDefaultForUser(address.getUserId());
            }
            address.setIsDefault(req.getIsDefault());
        }

        CustomerAddress saved = customerAddressRepository.save(address);
        return toDto(saved);
    }

    public List<CustomerAddressDTO> getAllByUserId(Long userId) {
        return customerAddressRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void deleteAddress(Long userId, Long id) {
        CustomerAddress address = customerAddressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        customerAddressRepository.delete(address);
    }

    private CustomerAddressDTO toDto(CustomerAddress a) {
        return CustomerAddressDTO.builder()
                .id(a.getId())
                .isDefault(Boolean.TRUE.equals(a.getIsDefault()))
                .provinceId(a.getProvinceId())
                .districtId(a.getDistrictId())
                .wardCode(a.getWardCode())
                .addressLine(a.getAddressLine())
                .receiverName(a.getReceiverName())
                .receiverPhone(a.getReceiverPhone())
                .label(a.getLabel())
                .build();
    }


//    private String buildFullAddress(CustomerAddress address) {
//        StringBuilder sb = new StringBuilder();
//        if (address.getAddressLine() != null && !address.getAddressLine().isBlank()) {
//            sb.append(address.getAddressLine());
//        }
//        Location loc = address.getLocation();
//        if (loc != null) {
//            if (loc.getName() != null && !loc.getName().isBlank()) {
//                if (!sb.isEmpty()) sb.append(", ");
//                sb.append(loc.getName());
//            }
//            Location parent = loc.getParent();
//            if (parent != null && parent.getName() != null && !parent.getName().isBlank()) {
//                if (!sb.isEmpty()) sb.append(", ");
//                sb.append(parent.getName());
//            }
//            Location grandParent = parent != null ? parent.getParent() : null;
//            if (grandParent != null && grandParent.getName() != null && !grandParent.getName().isBlank()) {
//                if (!sb.isEmpty()) sb.append(", ");
//                sb.append(grandParent.getName());
//            }
//        }
//        return sb.toString();
//    }
    public String getFullAddress(CustomerAddress a) {
        String province = ghnMasterDataService.getProvinceName(a.getProvinceId());
        String district = ghnMasterDataService.getDistrictName(a.getDistrictId());
        String ward = ghnMasterDataService.getWardName(a.getDistrictId(), a.getWardCode());

        return String.join(", ",
                a.getAddressLine() != null ? a.getAddressLine() : "",
                ward,
                district,
                province
        ).replaceAll(",\\s*,", ","); // remove empty parts
    }

}
