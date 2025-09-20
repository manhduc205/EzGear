package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByPhone(String phone);
}
