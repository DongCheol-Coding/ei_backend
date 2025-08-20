package com.example.ei_backend.mapper;

import com.example.ei_backend.domain.dto.PaymentDto;
import com.example.ei_backend.domain.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseName", source = "course.title")
    @Mapping(target = "price", source = "amount")
    @Mapping(target = "paymentDate", source = "approvedAt")
    PaymentDto toDto(Payment e);

    default String formatDate(Payment e) {
        var t = (e.getApprovedAt() != null) ? e.getApprovedAt() : e.getRequestedAt();
        return (t != null) ? t.toString() : null;
    }
}