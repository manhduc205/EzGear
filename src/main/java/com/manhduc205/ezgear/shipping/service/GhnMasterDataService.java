package com.manhduc205.ezgear.shipping.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manhduc205.ezgear.shipping.client.GhnRestClient;
import com.manhduc205.ezgear.shipping.dto.DistrictDTO;
import com.manhduc205.ezgear.shipping.dto.ProvinceDTO;
import com.manhduc205.ezgear.shipping.dto.WardDTO;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnMasterDataService {

    private final GhnRestClient client;
    private final Map<Integer, String> provinceCache = new ConcurrentHashMap<>();
    private final Map<Integer, String> districtCache = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, String>> wardCache = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Data
    static class GhnResponse {
        @JsonProperty("code")
        private int code;

        @JsonProperty("data")
        private List<Object> data;
    }
//    @PostConstruct
//    public void initCache() {
//
//        List<ProvinceDTO> provinces = getProvinces();
//        for (ProvinceDTO p : provinces) {
//            provinceCache.put(p.getId(), p.getName());
//        }
//
//        for (ProvinceDTO p : provinces) {
//            List<DistrictDTO> districts = getDistricts(p.getId());
//            for (DistrictDTO d : districts) {
//                districtCache.put(d.getId(), d.getName());
//            }
//        }
//
//        for (Map.Entry<Integer, String> entry : districtCache.entrySet()) {
//            Integer districtId = entry.getKey();
//            List<WardDTO> wards = getWards(districtId);
//            Map<String, String> wardMap = new HashMap<>();
//            for (WardDTO w : wards) {
//                wardMap.put(w.getCode(), w.getName());
//            }
//            wardCache.put(districtId, wardMap);
//        }
//    }
    public List<ProvinceDTO> getProvinces() {
        GhnResponse res = client.get("/master-data/province", GhnResponse.class);

        return res.getData().stream()
                .map(item -> mapper.convertValue(item, ProvinceDTO.class))
                .toList();
    }

    public List<DistrictDTO> getDistricts(Integer provinceId) {
        try {
            var res = client.get("/master-data/district", "province_id", provinceId, GhnResponse.class);
            if (res == null || res.getData() == null) {
                log.warn("GHN getDistricts returned null for provinceId={}", provinceId);
                return List.of();
            }
            return res.getData().stream()
                    .map(item -> mapper.convertValue(item, DistrictDTO.class))
                    .toList();

        } catch (Exception e) {
            log.error("GHN district API failed for provinceId={}", provinceId, e);
            return List.of();
        }
    }



    public List<WardDTO> getWards(Integer districtId) {
        try {
            var res = client.get("/master-data/ward", "district_id", districtId, GhnResponse.class);
            if (res == null || res.getData() == null) {
                log.warn("GHN getWards returned null for districtId={}", districtId);
                return List.of();
            }
            return res.getData().stream()
                    .map(item -> mapper.convertValue(item, WardDTO.class))
                    .toList();

        } catch (Exception e) {
            log.error("GHN ward API failed for districtId={}", districtId, e);
            return List.of();
        }
    }



    public String getProvinceName(Integer provinceId) {
        if (provinceId == null) return "";
        return provinceCache.getOrDefault(provinceId, "");
    }

    public String getDistrictName(Integer districtId) {
        if (districtId == null) return "";
        return districtCache.getOrDefault(districtId, "");
    }

    public String getWardName(Integer districtId, String wardCode) {
        if (districtId == null || wardCode == null) return "";

        Map<String, String> wards = wardCache.get(districtId);
        if (wards == null) return "";

        return wards.getOrDefault(wardCode, "");
    }
}
