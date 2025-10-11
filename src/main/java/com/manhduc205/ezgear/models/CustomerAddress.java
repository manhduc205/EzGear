package com.manhduc205.ezgear.models;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress extends AbstractEntity {
    //không thể để người dùng tự nhập locationcode
    // được tôi muốn sẽ chọn address line bằng combo box sau đó thì có thể sẽ get được locationcode ra từ đấy
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Nhà riêng, ông ty
    private String label;

    // Người nhận hàng
    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "location_code")
    private String locationCode;

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "is_default")
    private Boolean isDefault;

}

