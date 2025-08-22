package com.example.ei_backend.domain.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KakaoPayApproveResponseDto {
    private String aid;
    private String tid;
    private String cid;

    private String partnerOrderId;
    private String partnerUserId;

    private String itemName;
    private String paymentMethodType;

    private Amount amount;
    private String approvedAt; // 예: 2025-08-21T05:05:20Z

    @Getter @Setter
    @NoArgsConstructor
    public static class Amount {
        private int total;
        private int taxFree;
        private int vat;
        private int point;
        private int discount;
        private int greenDeposit;
    }
}