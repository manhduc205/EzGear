package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress,Long> {
    // địa chỉ mặc định của người dùng
    Optional<CustomerAddress> findByUserIdAndIsDefaultTrue(Long userId);
    Optional<CustomerAddress> findByIdAndUserId(Long id, Long userId);
    List<CustomerAddress> findByUserId(Long userId);
    @Modifying
    @Query("UPDATE CustomerAddress c SET c.isDefault = false WHERE c.userId = :userId")
    void clearDefaultForUser(Long userId);
}
