package com.example.ei_backend.service;

import com.example.ei_backend.domain.dto.KakaoPayApproveRequestDto;
import com.example.ei_backend.domain.dto.KakaoPayApproveResponseDto;
import com.example.ei_backend.domain.dto.KakaoPayReadyRequestDto;
import com.example.ei_backend.domain.dto.KakaoPayReadyResponseDto;
import com.example.ei_backend.domain.entity.*;
import com.example.ei_backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoPayService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final PendingPaymentRepository pendingPaymentRepository;
    private final PaymentRepository paymentRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Value("${kakaopay.api.secret.key}")
    private String kakaoPaySecretKey;

    @Value("${kakaopay.api.cid}")
    private String cid;

//    @Value("${app.client.host}")
//    private String clientHost;

    @Value("${app.front.success-url}")
    private String frontSuccessUrl;

    @Value("${app.client.host}") // 예: https://api.dongcheolcoding.life
    private String apiBaseUrl;


    @Transactional
    public KakaoPayReadyResponseDto ready(Course course, String userEmail) {
        try {
            var user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalStateException("사용자 없음"));

            // ✅ 이미 수강 중이면 Ready 자체를 차단
            if (userCourseRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
                throw new IllegalStateException("이미 결제(수강)한 코스입니다.");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "SECRET_KEY " + kakaoPaySecretKey);

            String orderId = UUID.randomUUID().toString();
            String approvalUrlWithOrderId = UriComponentsBuilder
                    .fromHttpUrl(frontSuccessUrl)
                    .queryParam("orderId", orderId)
                    .build(true)
                    .toUriString();

            KakaoPayReadyRequestDto request = KakaoPayReadyRequestDto.builder()
                    .cid(cid)
                    .partnerOrderId(orderId)
                    .partnerUserId(userEmail)
                    .itemName(course.getTitle())
                    .quantity(1)
                    .totalAmount(course.getPrice())
                    .taxFreeAmount(0)
                    .vatAmount(course.getPrice() / 10)
                    .approvalUrl(approvalUrlWithOrderId)
                    .cancelUrl(apiBaseUrl + "/api/payment/cancel")
                    .failUrl(apiBaseUrl + "/api/payment/fail")
                    .build();

            var entity = new HttpEntity<>(request, headers);
            var rawResponse = new RestTemplate().postForEntity(
                    "https://open-api.kakaopay.com/online/v1/payment/ready",
                    entity, String.class);

            var objectMapper = new ObjectMapper();
            var res = objectMapper.readValue(rawResponse.getBody(), KakaoPayReadyResponseDto.class);

            pendingPaymentRepository.save(
                    PendingPayment.builder()
                            .orderId(orderId)
                            .tid(res.getTid())
                            .userEmail(userEmail)
                            .courseId(course.getId())
                            .amount(course.getPrice())
                            .status(PaymentStatus.READY)
                            .build()
            );

            return res;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error(" [카카오 오류 응답] 상태코드: {}", e.getStatusCode());
            log.error(" [카카오 오류 응답 본문] {}", e.getResponseBodyAsString());
            throw new RuntimeException("카카오 API 오류: " + e.getMessage());
        } catch (Exception e) {
            log.error(" [예상치 못한 에러]", e);
            throw new RuntimeException("카카오페이 ready 요청 중 오류 발생", e);
        }
    }


    @Transactional
    public String approve(String orderId, String pgToken, String userEmail, Long userId) {
        PendingPayment pending = pendingPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("주문정보가 없습니다. 다시 시도해 주세요."));
        if (!pending.getUserEmail().equals(userEmail)) {
            throw new IllegalStateException("주문 소유자가 아닙니다.");
        }
        if (pending.getStatus() == PaymentStatus.APPROVED) {
            return "{\"message\":\"already approved\"}";
        }

        // ✅ 승인 호출 직전, 다시 보유 여부 점검
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자 없음"));
        var course = courseRepository.findById(pending.getCourseId())
                .orElseThrow(() -> new IllegalStateException("코스 없음"));

        if (userCourseRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            throw new IllegalStateException("이미 결제(수강)한 코스입니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + kakaoPaySecretKey);

        KakaoPayApproveRequestDto req = KakaoPayApproveRequestDto.builder()
                .cid(cid)
                .tid(pending.getTid())
                .partnerOrderId(orderId)
                .partnerUserId(userEmail)
                .pgToken(pgToken)
                .build();

        var resp = new RestTemplate().postForEntity(
                "https://open-api.kakaopay.com/online/v1/payment/approve",
                new HttpEntity<>(req, headers),
                KakaoPayApproveResponseDto.class
        );
        var body = Optional.ofNullable(resp.getBody())
                .orElseThrow(() -> new IllegalStateException("카카오 승인 응답이 비어있습니다."));
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("카카오 승인 실패: " + resp.getStatusCode());
        }

        int approvedAmount = body.getAmount().getTotal();
        if (approvedAmount != pending.getAmount()) {
            throw new IllegalStateException("금액 불일치");
        }
        LocalDateTime approvedAt = parseKakaoTime(body.getApprovedAt());
        log.info("[KakaoPay][APPROVE] approved_at(raw)={}", body.getApprovedAt());

        // 수강 등록(멱등)
        if (!userCourseRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            userCourseRepository.save(UserCourse.builder()
                    .user(user)
                    .course(course)
                    .registeredAt(LocalDateTime.now())
                    .build());
        }

        if (!paymentRepository.existsByTid(pending.getTid())) {
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .tid(pending.getTid())
                    .pgTid(body.getTid())
                    .user(user)
                    .course(course)
                    .amount(approvedAmount)
                    .method(PaymentMethod.KAKAOPAY)
                    .status(PaymentStatus.APPROVED)
                    .paymentDate(approvedAt)
                    .approvedAt(approvedAt)
                    .build();
            paymentRepository.save(payment);
        }

        pending.setStatus(PaymentStatus.APPROVED);
        return "{\"message\":\"approved\",\"tid\":\"" + body.getTid() + "\"}";
    }

    private LocalDateTime parseKakaoTime(String ts) {
        if (ts == null || ts.isBlank()) return LocalDateTime.now(KST);
        try {
            // '2025-08-24T04:00:53+09:00' 같은 오프셋 포함 문자열
            return OffsetDateTime.parse(ts).atZoneSameInstant(KST).toLocalDateTime();
        } catch (DateTimeParseException e) {
            // '2025-08-24T04:00:53' 같이 오프셋 없는 문자열
            return LocalDateTime.parse(ts, ISO_LOCAL);
        }
    }
}

