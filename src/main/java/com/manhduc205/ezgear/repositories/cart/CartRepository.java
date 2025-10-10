package com.manhduc205.ezgear.repositories.cart;

import com.manhduc205.ezgear.models.cart.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByUserId(Long userId);
}
