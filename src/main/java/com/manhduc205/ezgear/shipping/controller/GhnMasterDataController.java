package com.manhduc205.ezgear.shipping.controller;

import com.manhduc205.ezgear.shipping.service.GhnMasterDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
public class GhnMasterDataController {

    private final GhnMasterDataService ghnService;

    @GetMapping("/provinces")
    public Object getProvinces() {
        return ghnService.getProvinces();
    }

    @GetMapping("/districts")
    public Object getDistricts(@RequestParam Integer provinceId) {
        return ghnService.getDistricts(provinceId);
    }

    @GetMapping("/wards")
    public Object getWards(@RequestParam Integer districtId) {
        return ghnService.getWards(districtId);
    }
}
