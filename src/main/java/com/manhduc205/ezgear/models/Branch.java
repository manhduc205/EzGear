package com.manhduc205.ezgear.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "province")
    private String province;

    @Column(name = "address_line")
    private String addressLine;

    @Column(length = 32)
    private String phone;

    @Column(name = "is_active")
    private Boolean isActive = true;

}
