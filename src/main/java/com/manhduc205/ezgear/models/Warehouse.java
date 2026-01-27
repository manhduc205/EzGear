package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "warehouses")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Warehouse extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "map_url", columnDefinition = "TEXT")
    private String mapUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
