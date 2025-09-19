package com.manhduc205.ezgear.models;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "tokens")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Token extends AbstractEntity{

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "token_type", nullable = false, length = 50)
    private String tokenType;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "expired", nullable = false)
    private boolean expired;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
