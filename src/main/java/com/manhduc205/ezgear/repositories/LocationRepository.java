package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Integer> {
    Optional<Location> findByCode(String code);
}
