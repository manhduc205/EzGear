package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress,Long> {
    // địa chỉ mặc định của người dùng
    Optional<CustomerAddress> findByUserIdAndIsDefaultTrue(Long userId);

}
