package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "branches")
public class Branch extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    // GHN mapping fields
    @Column(name = "province_id", nullable = false)
    private Integer provinceId;

    @Column(name = "district_id", nullable = false)
    private Integer districtId;

    @Column(name = "ward_code", nullable = false, length = 50)
    private String wardCode;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(length = 32)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
