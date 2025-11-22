package com.manhduc205.ezgear.shipping.location_ghn;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ghn_locations")
@Getter
@Setter
public class GhnLocation {

    @Id
    private String id;   // VARCHAR(20)

    private String name;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name = "parent_id")
    private String parentId;

    public enum Type {
        PROVINCE, DISTRICT, WARD
    }
}

