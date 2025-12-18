package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Role;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findById(Long id);

}
