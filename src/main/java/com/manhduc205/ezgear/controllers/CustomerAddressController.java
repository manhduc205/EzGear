package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.CustomerAddressRequest;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.services.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer-addresses")
@RequiredArgsConstructor
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    @PostMapping
    public ResponseEntity<CustomerAddress> create(@RequestBody CustomerAddressRequest request) {
        CustomerAddress saved = customerAddressService.createAddress(request);
        return ResponseEntity.ok(saved);
    }
}
