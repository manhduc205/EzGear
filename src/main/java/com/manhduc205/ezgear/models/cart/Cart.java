package com.manhduc205.ezgear.models.cart;


import com.manhduc205.ezgear.models.User;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "carts")
public class Cart {
    @Id
    private String id;

    private Long userId;

    private Map<Long, CartItem> items = new ConcurrentHashMap<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
