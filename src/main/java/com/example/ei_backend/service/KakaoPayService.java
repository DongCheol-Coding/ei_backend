package com.example.ei_backend.service;

import com.example.ei_backend.domain.dto.KakaoPayApproveRequestDto;
import com.example.ei_backend.domain.dto.KakaoPayApproveResponseDto;
import com.example.ei_backend.domain.dto.KakaoPayReadyRequestDto;
import com.example.ei_backend.domain.dto.KakaoPayReadyResponseDto;
import com.example.ei_backend.domain.entity.*;
import com.example.ei_backend.repository.*;
import com.example.ei_backend.util.AppFrontProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoPayService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final PendingPaymentRepository pendingPaymentRepository;
    private final PaymentRepository paymentRepository;
    private final AppFrontProperties frontProps;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Value("${kakaopay.api.secret.key}")
    private String kakaoPaySecretKey;

    @Value("${kakaopay.api.cid}")
    private String cid;

    @PostConstruct
    void logPaymentUrls() {
        var p = frontProps.getPayment();
        log.info("[PAYMENT URLS] successUrl={}, failUrl={}",
                p != null ? p.getSuccessUrl() : null,
                p != null ? p.getFailUrl() : null);
    }

    @Transactional
    public KakaoPayReadyResponseDto ready(Course course, String userEmail) {
        try {
            var user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalStateException("사용자 없음"));

            if (userCourseRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
                throw new IllegalStateException("이미 결제(수강)한 코스입니다.");
            }

            // (필요 시) 실패 URL만 설정값에서 사용
            var failUrl = Optional.ofNullable(frontProps.getPayment().getFailUrl())
                    .orElseThrow(() -> new IllegalStateException("결제 실패 URL 설정이 필요합니다"));
            if (!isLocalUrl(failUrl) && !failUrl.startsWith("https://")) {
                throw new IllegalStateException("HTTPS 결제 실패 URL이 필요합니다");
            }

            //  여기서 orderId 먼저 생성
            String orderId = UUID.randomUUID().toString();

            //  성공 콜백은 백엔드로 (orderId를 쿼리에 붙임)
            String approvalUrlWithOrderId = UriComponentsBuilder
                    .fromUriString("https://api.dongcheolcoding.life/api/payment/kakaopay/callback/success")
                    .queryParam("orderId", orderId)
                    .build(true)
                    .toUriString();

            // 취소/실패 URL은 안전하게 조립
            String cancelUrl = UriComponentsBuilder.fromUriString(failUrl)
                    .queryParam("reason", "cancelled")
                    .build(true)
                    .toUriString();
            String failUrlWithReason = UriComponentsBuilder.fromUriString(failUrl)
                    .queryParam("reason", "failed")
                    .build(true)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "SECRET_KEY " + kakaoPaySecretKey);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            String itemName = Optional.ofNullable(course.getTitle()).orElse("Course");
            if (itemName.length() > 100) itemName = itemName.substring(0, 100);

            var request = KakaoPayReadyRequestDto.builder()
                    .cid(cid)
                    .partnerOrderId(orderId)
                    .partnerUserId(userEmail)
                    .itemName(itemName)
                    .quantity(1)
                    .totalAmount(course.getPrice())
                    .taxFreeAmount(0)
                    .vatAmount(course.getPrice() / 10)
                    .approvalUrl(approvalUrlWithOrderId)     //  백엔드 콜백
                    .cancelUrl(cancelUrl)
                    .failUrl(failUrlWithReason)
                    .build();

            var entity = new HttpEntity<>(request, headers);
            var rawResponse = restTemplate.postForEntity(
                    "https://open-api.kakaopay.com/online/v1/payment/ready",
                    entity, String.class);

            if (!rawResponse.getStatusCode().is2xxSuccessful() || rawResponse.getBody() == null) {
                throw new IllegalStateException("카카오 ready 응답이 비정상입니다: " + rawResponse.getStatusCode());
            }

            var res = objectMapper.readValue(rawResponse.getBody(), KakaoPayReadyResponseDto.class);
            if (res.getTid() == null) throw new IllegalStateException("카카오 ready 응답에 tid가 없습니다.");

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

        } catch (Exception e) {
            log.error("[KAKAO][READY] 에러", e);
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

        // 승인 호출 직전, 다시 보유 여부 점검
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

        var resp = restTemplate.postForEntity(
                "https://open-api.kakaopay.com/online/v1/payment/approve",
                new HttpEntity<>(req, headers),
                KakaoPayApproveResponseDto.class
        );

        // 응답 방어
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("카카오 승인 응답이 비정상입니다: " + resp.getStatusCode());
        }
        var body = resp.getBody();

        if (body.getAmount() == null) {
            throw new IllegalStateException("승인 응답에 amount가 없습니다.");
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

    private boolean isLocalUrl(String url) {
        return url.startsWith("http://localhost") || url.startsWith("http://127.0.0.1");
    }
}
