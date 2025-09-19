package com.manhduc205.ezgear.models;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Table(name = "roles")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends AbstractEntity{
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;


}
