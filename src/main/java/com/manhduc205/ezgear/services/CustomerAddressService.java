package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.CustomerAddressRequest;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Location;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final LocationRepository locationRepository;

    public CustomerAddress createAddress(CustomerAddressRequest req) {
        Location location = locationRepository.findByCode(req.getLocationCode())
                .orElseThrow(() -> new RuntimeException("Location not found"));

        CustomerAddress address = CustomerAddress.builder()
                .userId(req.getUserId())
                .receiverName(req.getReceiverName())
                .receiverPhone(req.getReceiverPhone())
                .addressLine(req.getAddressLine())
                .label(req.getLabel())
                .isDefault(req.getIsDefault() != null && req.getIsDefault())
                .location(location)
                .build();

        return customerAddressRepository.save(address);
    }

    public CustomerAddress updateAddress(Long id, CustomerAddressRequest req) {
        // Tìm địa chỉ theo id
        CustomerAddress address = customerAddressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer address not found"));

        // Nếu locationCode thay đổi thì load lại Location
        if (req.getLocationCode() != null && (address.getLocation() == null || !req.getLocationCode().equals(address.getLocation().getCode()))) {
            Location newLocation = locationRepository.findByCode(req.getLocationCode())
                    .orElseThrow(() -> new RuntimeException("Location not found"));
            address.setLocation(newLocation);
        }

        // Cập nhật các trường khác nếu có
        if (req.getReceiverName() != null) {
            address.setReceiverName(req.getReceiverName());
        }
        if (req.getReceiverPhone() != null) {
            address.setReceiverPhone(req.getReceiverPhone());
        }
        if (req.getAddressLine() != null) {
            address.setAddressLine(req.getAddressLine());
        }
        if (req.getLabel() != null) {
            address.setLabel(req.getLabel());
        }
        if (req.getIsDefault() != null) {
            address.setIsDefault(req.getIsDefault());
        }

        return customerAddressRepository.save(address);
    }

    public List<CustomerAddress> getAllByUserId(Long userId) {
        return customerAddressRepository.findByUserId(userId);
    }
    public void deleteAddress(Long userId, Long id) {
        CustomerAddress address = customerAddressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        customerAddressRepository.delete(address);
    }

}
