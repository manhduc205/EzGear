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

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone", length = 32)
    private String receiverPhone;

    @Column(name = "location_code", length = 20)
    private String locationCode;

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}
