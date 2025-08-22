package com.example.ei_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT (MySQL/MariaDB)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String tid;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    public void markApproved()   { this.status = PaymentStatus.APPROVED; }
    public void markCancelled()  { this.status = PaymentStatus.CANCELLED; }
    public void markFailed()     { this.status = PaymentStatus.FAILED; }
}