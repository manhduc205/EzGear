package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.responses.LocationOptionResponse;
import com.manhduc205.ezgear.services.GhnLocalLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final GhnLocalLocationService locationService;

    @GetMapping("")
    public ResponseEntity<List<LocationOptionResponse>> getProvinces() {
        List<LocationOptionResponse> provinces = locationService.getAllProvinces();
        return ResponseEntity.ok(provinces);
    }
}