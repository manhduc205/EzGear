package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
}
