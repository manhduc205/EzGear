package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.order.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
}

