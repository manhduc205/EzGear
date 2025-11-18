package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.CustomerAddressDTO;
import com.manhduc205.ezgear.dtos.request.CustomerAddressRequest;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Location;
import com.manhduc205.ezgear.repositories.CustomerAddressRepository;
import com.manhduc205.ezgear.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final LocationRepository locationRepository;

    public CustomerAddressDTO createAddress(CustomerAddressRequest req) {
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

        CustomerAddress saved = customerAddressRepository.save(address);
        return toDto(saved);
    }

    public CustomerAddressDTO updateAddress(Long id, CustomerAddressRequest req) {
        CustomerAddress address = customerAddressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer address not found"));

        if (req.getLocationCode() != null && (address.getLocation() == null || !req.getLocationCode().equals(address.getLocation().getCode()))) {
            Location newLocation = locationRepository.findByCode(req.getLocationCode())
                    .orElseThrow(() -> new RuntimeException("Location not found"));
            address.setLocation(newLocation);
        }

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

    private CustomerAddressDTO toDto(CustomerAddress address) {
        return CustomerAddressDTO.builder()
                .id(address.getId())
                .fullAddress(buildFullAddress(address))
                .isDefault(Boolean.TRUE.equals(address.getIsDefault()))
                .build();
    }

    /**
     * Public helper so other services (e.g. checkout) can reuse full address building
     * logic without duplicating code or exposing JPA entities directly.
     */
    public String getFullAddress(CustomerAddress address) {
        return buildFullAddress(address);
    }

    private String buildFullAddress(CustomerAddress address) {
        StringBuilder sb = new StringBuilder();
        if (address.getAddressLine() != null && !address.getAddressLine().isBlank()) {
            sb.append(address.getAddressLine());
        }
        Location loc = address.getLocation();
        if (loc != null) {
            if (loc.getName() != null && !loc.getName().isBlank()) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(loc.getName());
            }
            Location parent = loc.getParent();
            if (parent != null && parent.getName() != null && !parent.getName().isBlank()) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(parent.getName());
            }
            Location grandParent = parent != null ? parent.getParent() : null;
            if (grandParent != null && grandParent.getName() != null && !grandParent.getName().isBlank()) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(grandParent.getName());
            }
        }
        return sb.toString();
    }
}
