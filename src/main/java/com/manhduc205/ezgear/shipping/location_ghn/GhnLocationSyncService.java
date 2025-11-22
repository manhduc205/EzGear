package com.manhduc205.ezgear.shipping.location_ghn;

import com.manhduc205.ezgear.shipping.service.GhnMasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnLocationSyncService {

    private final GhnMasterDataService ghnService;
    private final GhnLocationRepository repo;

    @Transactional
    public void syncAll() {
        try {
            log.info("[GHN Sync] Start syncAll: clearing ghn_locations");
            repo.deleteAll();

            log.info("[GHN Sync] Sync provinces...");
            syncProvinces();

            log.info("[GHN Sync] Sync districts...");
            syncDistricts();

            log.info("[GHN Sync] Sync wards...");
            syncWards();

            log.info("[GHN Sync] DONE");
        } catch (Exception e) {
            log.error("[GHN Sync] FAILED", e);
            throw e; // để GlobalExceptionHandler trả về 500 với stacktrace
        }
    }

    private void syncProvinces() {
        var provinces = ghnService.getProvinces();
        log.info("[GHN Sync] Provinces from GHN: {}", provinces.size());

        provinces.forEach(p -> {
            GhnLocation loc = new GhnLocation();
            loc.setId(String.valueOf(p.getId()));   // ProvinceID
            loc.setName(p.getName());               // ProvinceName
            loc.setType(GhnLocation.Type.PROVINCE);
            loc.setParentId(null);
            repo.save(loc);
        });
    }

    private void syncDistricts() {
        var provinces = repo.findByType(GhnLocation.Type.PROVINCE);
        log.info("[GHN Sync] Sync districts for {} provinces", provinces.size());

        provinces.forEach(province -> {
            Integer provinceId = Integer.valueOf(province.getId());
            var districts = ghnService.getDistricts(provinceId); // luôn != null, có thể empty

            if (districts.isEmpty()) {
                log.warn("[GHN Sync] Province {} ({}) has NO districts from GHN", province.getId(), province.getName());
                return;
            }

            districts.forEach(d -> {
                GhnLocation loc = new GhnLocation();
                loc.setId(String.valueOf(d.getId()));   // DistrictID
                loc.setName(d.getName());               // DistrictName
                loc.setType(GhnLocation.Type.DISTRICT);
                loc.setParentId(province.getId());
                repo.save(loc);
            });
        });
    }

    private void syncWards() {
        var districts = repo.findByType(GhnLocation.Type.DISTRICT);
        log.info("[GHN Sync] Sync wards for {} districts", districts.size());

        districts.forEach(district -> {
            Integer districtId = Integer.valueOf(district.getId());
            var wards = ghnService.getWards(districtId); // luôn != null, có thể empty

            if (wards.isEmpty()) {
                log.warn("[GHN Sync] District {} ({}) has NO wards from GHN", district.getId(), district.getName());
                return;
            }

            wards.forEach(w -> {
                GhnLocation loc = new GhnLocation();
                loc.setId(w.getCode());              // WardCode
                loc.setName(w.getName());            // WardName
                loc.setType(GhnLocation.Type.WARD);
                loc.setParentId(district.getId());
                repo.save(loc);
            });
        });
    }
}
