package com.manhduc205.ezgear.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "branches")
public class Branch extends AbstractEntity{

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    // Liên kết đến bảng locations qua location_code
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_code", referencedColumnName = "code", nullable = false)
    private Location location;

    @Column(name = "address_line")
    private String addressLine;

    @Column(length = 32)
    private String phone;

    @Column(name = "is_active")
    private Boolean isActive = true;

}
