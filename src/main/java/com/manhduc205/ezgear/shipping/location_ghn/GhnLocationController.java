package com.manhduc205.ezgear.shipping.location_ghn;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ghn-locations")
@RequiredArgsConstructor
public class GhnLocationController {

    // TODO: Mục tiêu của controller này là lưu các dữ liệu từ api GHN vào db của EzGear để sử dụng lại, tránh việc gọi API nhiều lần gây chậm
    // Chỉ chạy 1 lần duy nhất khi cần đồng bộ dữ liệu từ GHN vào db
    private final GhnLocationRepository repo;

    @GetMapping("/provinces")
    public List<GhnLocationDTO> provinces() {
        return repo.findByType(GhnLocation.Type.PROVINCE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/districts")
    public List<GhnLocationDTO> districts(@RequestParam Integer provinceId) {
        return repo.findByParentId(String.valueOf(provinceId)).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/wards")
    public List<GhnLocationDTO> wards(@RequestParam Integer districtId) {
        return repo.findByParentId(String.valueOf(districtId)).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private GhnLocationDTO convertToDto(GhnLocation loc) {
        return new GhnLocationDTO(loc.getId(), loc.getName(), loc.getType(), loc.getParentId());
    }
}
