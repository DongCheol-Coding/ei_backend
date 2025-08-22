package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.PendingPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingPaymentRepository extends JpaRepository<PendingPayment, Long> {
    Optional<PendingPayment> findByOrderId(String orderId);
}