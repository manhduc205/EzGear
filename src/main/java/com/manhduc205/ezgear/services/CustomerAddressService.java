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

        String fullAddr = generateFullAddress(
                req.getAddressLine(), req.getProvinceId(), req.getDistrictId(), req.getWardCode()
        );
        address.setFullAddress(fullAddr);
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

        if (req.getReceiverName() != null) address.setReceiverName(req.getReceiverName());
        if (req.getReceiverPhone() != null) address.setReceiverPhone(req.getReceiverPhone());
        if (req.getLabel() != null) address.setLabel(req.getLabel());

        boolean isLocationChanged = false;

        if (req.getProvinceId() != null) {
            address.setProvinceId(req.getProvinceId());
            isLocationChanged = true;
        }
        if (req.getDistrictId() != null) {
            address.setDistrictId(req.getDistrictId());
            isLocationChanged = true;
        }
        if (req.getWardCode() != null) {
            address.setWardCode(req.getWardCode());
            isLocationChanged = true;
        }
        if (req.getAddressLine() != null) {
            address.setAddressLine(req.getAddressLine());
            isLocationChanged = true;
        }

        if (isLocationChanged) {
            String newFullAddress = generateFullAddress(
                    address.getAddressLine(), address.getProvinceId(), address.getDistrictId(), address.getWardCode()
            );
            address.setFullAddress(newFullAddress);
        }

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
                .fullAddress(customerAddress.getFullAddress())
                .receiverName(customerAddress.getReceiverName())
                .receiverPhone(customerAddress.getReceiverPhone())
                .label(customerAddress.getLabel())
                .build();
    }

    private String generateFullAddress(String specificAddress, Integer provinceId, Integer districtId, String wardCode) {
        String province = ghnLocalLocationService.getProvinceName(provinceId);
        String district = ghnLocalLocationService.getDistrictName(districtId);
        String ward = ghnLocalLocationService.getWardName(wardCode);

        return String.join(", ",
                specificAddress != null ? specificAddress : "",
                ward,
                district,
                province
        ).replaceAll("^, |, , ", "");
    }
}
