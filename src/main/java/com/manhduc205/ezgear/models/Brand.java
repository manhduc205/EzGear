package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "brands")
public class Brand extends AbstractEntity {

    @Column(nullable = false,unique = true)
    private String name;

    @Column( name = "slug", nullable = false,unique = true)
    private String slug;

}
