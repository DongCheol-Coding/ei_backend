package com.example.ei_backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)   // ★예상치 못한 필드 무시
public class KakaoPayApproveResponseDto {
    private String aid;
    private String tid;
    private String cid;

    private String partnerOrderId;
    private String partnerUserId;

    private String itemName;
    private String itemCode;
    private Integer quantity;

    private String paymentMethodType;
    private Amount amount;

    private String createdAt;                 // (옵션) 올 수 있음
    private String approvedAt;

    @Getter @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private int total;
        private int taxFree;
        private int vat;
        private int point;
        private int discount;
        private int greenDeposit;
    }
}
