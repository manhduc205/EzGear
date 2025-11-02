package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.CustomerAddressRequest;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Location;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
