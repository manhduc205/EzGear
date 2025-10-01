package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "warehouses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Warehouse extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active")
    private Boolean isActive = true;

}

