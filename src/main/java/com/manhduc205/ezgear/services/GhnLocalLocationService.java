package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.responses.LocationOptionResponse;
import com.manhduc205.ezgear.shipping.location_ghn.GhnLocation;
import com.manhduc205.ezgear.shipping.location_ghn.GhnLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GhnLocalLocationService {

    private final GhnLocationRepository ghnLocationRepository;

    public List<LocationOptionResponse> getAllProvinces() {
        return ghnLocationRepository.findAllByType(GhnLocation.Type.PROVINCE)
                .stream()
                .map(loc -> LocationOptionResponse.builder()
                        .id(Integer.parseInt(loc.getId()))
                        .name(loc.getName())
                        .build())

                .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))
                .collect(Collectors.toList());
    }

    public String getProvinceName(Integer id) {
        return ghnLocationRepository.findByTypeAndId(GhnLocation.Type.PROVINCE, String.valueOf(id))
                .map(GhnLocation::getName)
                .orElse("N/A");
    }

    public String getDistrictName(Integer id) {
        return ghnLocationRepository.findByTypeAndId(GhnLocation.Type.DISTRICT, String.valueOf(id))
                .map(GhnLocation::getName)
                .orElse("N/A");
    }

    public String getWardName(String wardCode) {
        return ghnLocationRepository.findByTypeAndId(GhnLocation.Type.WARD, String.valueOf(wardCode))
                .map(GhnLocation::getName)
                .orElse("N/A");
    }
}

