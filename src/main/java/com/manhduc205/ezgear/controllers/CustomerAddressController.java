package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.CustomerAddressRequest;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer-addresses")
@RequiredArgsConstructor
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    //Tạo địa chỉ mới cho user hiện tại
    @PostMapping
    public ResponseEntity<?> createAddress(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CustomerAddressRequest request
    ) {
        request.setUserId(user.getId());
        CustomerAddress address = customerAddressService.createAddress(request);
        return ResponseEntity.ok(address);
    }

    //Cập nhật địa chỉ
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody CustomerAddressRequest request) {
        request.setUserId(user.getId());
        CustomerAddress updated = customerAddressService.updateAddress(id, request);
        return ResponseEntity.ok(updated);
    }

    // Lấy danh sách địa chỉ của user hiện tại
    @GetMapping
    public ResponseEntity<?> getAllAddresses(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        List<CustomerAddress> addresses = customerAddressService.getAllByUserId(user.getId());
        return ResponseEntity.ok(addresses);
    }

    //Xóa địa chỉ
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id
    ) {
        customerAddressService.deleteAddress(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
