package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.Payment;
import com.example.ei_backend.domain.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByUser_IdAndCourse_IdAndStatus(Long userId, Long courseId, PaymentStatus status);
    List<Payment> findByUserIdOrderByIdDesc(Long userId);

    boolean existsByTid(String tid);

    // 결제 + 코스 한 번에 가져오기 (N+1 방지)
    @Query("""
           select p
           from Payment p
           join fetch p.course c
           where p.user.id = :userId
             and p.status = com.example.ei_backend.domain.entity.PaymentStatus.APPROVED
           order by p.paymentDate desc
           """)
    List<Payment> findApprovedByUserIdWithCourse(@Param("userId") Long userId);
}