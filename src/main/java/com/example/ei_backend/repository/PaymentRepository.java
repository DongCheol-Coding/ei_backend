package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.Payment;
import com.example.ei_backend.domain.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByUser_IdAndCourse_IdAndStatus(Long userId, Long courseId, PaymentStatus status);
    List<Payment> findByUserIdOrderByIdDesc(Long userId);

    boolean existsByTid(String tid);
}