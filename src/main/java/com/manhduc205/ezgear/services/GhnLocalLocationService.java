package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.shipping.location_ghn.GhnLocation;
import com.manhduc205.ezgear.shipping.location_ghn.GhnLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GhnLocalLocationService {

    private final GhnLocationRepository ghnLocationRepository;

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

