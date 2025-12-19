package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress extends AbstractEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone", length = 32, nullable = false)
    private String receiverPhone;

    // GHN IDs
    @Column(name = "province_id", nullable = false)
    private Integer provinceId;

    @Column(name = "district_id", nullable = false)
    private Integer districtId;

    @Column(name = "ward_code", nullable = false, length = 20)
    private String wardCode;

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "full_address")
    private String fullAddress;

    @Column(name = "label")
    private String label; // Nhà riêng / Văn phòng

    @Column(name = "is_default")
    private Boolean isDefault = false;
}
