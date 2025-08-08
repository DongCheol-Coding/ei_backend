package com.example.ei_backend.service;

import com.example.ei_backend.domain.dto.KakaoPayApproveRequestDto;
import com.example.ei_backend.domain.dto.KakaoPayReadyRequestDto;
import com.example.ei_backend.domain.dto.KakaoPayReadyResponseDto;
import com.example.ei_backend.domain.entity.Course;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoPayService {

    @Value("${kakaopay.api.secret.key}")
    private String kakaoPaySecretKey;

    @Value("${kakaopay.api.cid}")
    private String cid;

    @Value("${client.host}")
    private String clientHost;

    private String tid; // 실제 서비스에서는 DB에 매핑 저장해야 함

    private final Map<String, String> userTidMap = new ConcurrentHashMap<>();
    private final Map<String, String> userOrderIdMap = new ConcurrentHashMap<>();


    public KakaoPayReadyResponseDto ready(Course course, String userEmail) {
        try {
            log.info("🔥 [KakaoPay] Admin Key: {}", kakaoPaySecretKey);
            log.info("🔥 [KakaoPay] CID: {}", cid);

            log.info("🔥 course title = {}", course.getTitle());
            log.info("🔥 course price = {}", course.getPrice());
            log.info("🔥 userEmail = {}", userEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "SECRET_KEY " + kakaoPaySecretKey);
            log.info("🔥 Authorization 헤더: {}", headers.getFirst("Authorization"));

            String orderId = UUID.randomUUID().toString();

            KakaoPayReadyRequestDto request = KakaoPayReadyRequestDto.builder()
                    .cid(cid)
                    .partnerOrderId(orderId)
                    .partnerUserId(userEmail)
                    .itemName(course.getTitle())
                    .quantity(1)
                    .totalAmount(course.getPrice())
                    .taxFreeAmount(0)
                    .vatAmount(course.getPrice() / 10)
                    .approvalUrl(clientHost + "/api/payments/approve")
                    .cancelUrl(clientHost + "/api/payments/cancel")
                    .failUrl(clientHost + "/api/payments/fail")
                    .build();

            HttpEntity<KakaoPayReadyRequestDto> entity = new HttpEntity<>(request, headers);

            // ✅ 우선 String으로 응답 받아서 로그 확인
            ResponseEntity<String> rawResponse = new RestTemplate().postForEntity(
                    "https://open-api.kakaopay.com/online/v1/payment/ready",
                    entity,
                    String.class
            );

            log.info("📦 [카카오 응답 원문] {}", rawResponse.getBody());

            // ✅ 응답을 DTO로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            KakaoPayReadyResponseDto responseDto = objectMapper.readValue(
                    rawResponse.getBody(),
                    KakaoPayReadyResponseDto.class
            );

            userTidMap.put(userEmail, responseDto.getTid());
            userOrderIdMap.put(userEmail, orderId);

            return responseDto;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ [카카오 오류 응답] 상태코드: {}", e.getStatusCode());
            log.error("❌ [카카오 오류 응답 본문] {}", e.getResponseBodyAsString());
            throw new RuntimeException("카카오 API 오류: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ [예상치 못한 에러]", e);
            throw new RuntimeException("카카오페이 ready 요청 중 오류 발생", e);
        }
    }

    public String approve(String pgToken, String userEmail) {
        String tid = userTidMap.get(userEmail);
        String orderId = userOrderIdMap.get(userEmail);

        if (tid == null || orderId == null) {
            throw new IllegalStateException("TID 또는 주문번호가 존재하지 않습니다. 결제를 다시 시도해주세요.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + kakaoPaySecretKey);

        KakaoPayApproveRequestDto request = KakaoPayApproveRequestDto.builder()
                .cid(cid)
                .tid(tid)
                .partnerOrderId(orderId)
                .partnerUserId(userEmail)
                .pgToken(pgToken)
                .build();

        HttpEntity<KakaoPayApproveRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = new RestTemplate().postForEntity(
                "https://open-api.kakaopay.com/online/v1/payment/approve",
                entity,
                String.class
        );

        // ✅ 이후에: 결제 정보 DB 저장, 강의 수강 등록 처리
        return response.getBody();
    }

}



