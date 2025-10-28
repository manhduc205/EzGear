package com.manhduc205.ezgear.shipping.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.manhduc205.ezgear.shipping.client.GhnRestClient;
import com.manhduc205.ezgear.shipping.dto.DistrictDTO;
import com.manhduc205.ezgear.shipping.dto.ProvinceDTO;
import com.manhduc205.ezgear.shipping.dto.WardDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GhnMasterDataService {

    private final GhnRestClient client;

    @Data
    static class GhnResponse<T> {
        @JsonProperty("code")
        private int code;

        @JsonProperty("data")
        private List<T> data;
    }

    public List<ProvinceDTO> getProvinces() {
        GhnResponse<ProvinceDTO> res = client.get("/master-data/province", GhnResponse.class);
        return res.getData();
    }

    public List<DistrictDTO> getDistricts(Integer provinceId) {
        GhnResponse<DistrictDTO> res = client.get("/master-data/district", "province_id", provinceId, GhnResponse.class);
        return res.getData();
    }

    public List<WardDTO> getWards(Integer districtId) {
        GhnResponse<WardDTO> res = client.get("/master-data/ward", "district_id", districtId, GhnResponse.class);
        return res.getData();
    }
}
