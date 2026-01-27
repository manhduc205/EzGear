package com.manhduc205.ezgear.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;


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
    @ManyToMany(mappedBy = "brands", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
}
