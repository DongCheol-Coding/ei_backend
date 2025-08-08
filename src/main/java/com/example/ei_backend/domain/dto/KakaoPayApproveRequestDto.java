package com.example.ei_backend.domain.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KakaoPayApproveRequestDto {

    private String cid;              // 가맹점 코드
    private String tid;              // 결제 고유번호 (ready 응답에서 받은 값)
    private String partnerOrderId;   // 가맹점 주문번호
    private String partnerUserId;    // 가맹점 회원 ID
    private String pgToken;          // 카카오페이에서 전달받은 pg_token
}