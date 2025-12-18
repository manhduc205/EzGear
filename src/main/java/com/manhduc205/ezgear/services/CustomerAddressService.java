package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.CustomerAddressDTO;
import com.manhduc205.ezgear.dtos.request.CustomerAddressRequest;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final GhnLocalLocationService ghnLocalLocationService;
    @Transactional
    public CustomerAddressDTO createAddress(CustomerAddressRequest req) {

        if (req.getProvinceId() == null || req.getDistrictId() == null || req.getWardCode() == null) {
            throw new RequestException("Thiếu thông tin tỉnh / quận / phường");
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
                .orElseThrow(() -> new RequestException("Customer address not found"));

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
                .orElseThrow(() -> new RequestException("Address not found"));
        customerAddressRepository.delete(address);
    }

    private CustomerAddressDTO toDto(CustomerAddress customerAddress) {
        return CustomerAddressDTO.builder()
                .id(customerAddress.getId())
                .isDefault(Boolean.TRUE.equals(customerAddress.getIsDefault()))
                .provinceId(customerAddress.getProvinceId())
                .districtId(customerAddress.getDistrictId())
                .wardCode(customerAddress.getWardCode())
                .addressLine(customerAddress.getAddressLine())
                .receiverName(customerAddress.getReceiverName())
                .receiverPhone(customerAddress.getReceiverPhone())
                .label(customerAddress.getLabel())
                .build();
    }

    public String getFullAddress(CustomerAddress customerAddress) {
        String province = ghnLocalLocationService.getProvinceName(customerAddress.getProvinceId());
        String district = ghnLocalLocationService.getDistrictName(customerAddress.getDistrictId());
        String ward = ghnLocalLocationService.getWardName(customerAddress.getWardCode());

        return String.join(", ",
                customerAddress.getAddressLine() != null ? customerAddress.getAddressLine() : "",
                ward,
                district,
                province
        ).replaceAll(",\\s*,", ",");
    }
}
