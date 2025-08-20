package com.example.ei_backend.service;

import com.example.ei_backend.domain.dto.KakaoPayApproveRequestDto;
import com.example.ei_backend.domain.dto.KakaoPayReadyRequestDto;
import com.example.ei_backend.domain.dto.KakaoPayReadyResponseDto;
import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.domain.entity.UserCourse;
import com.example.ei_backend.repository.CourseRepository;
import com.example.ei_backend.repository.UserCourseRepository;
import com.example.ei_backend.repository.UserRepository;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoPayService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;

    @Value("${kakaopay.api.secret.key}")
    private String kakaoPaySecretKey;

    @Value("${kakaopay.api.cid}")
    private String cid;

    @Value("${app.client.host}")
    private String clientHost;

    private final Map<String, String> orderIdByUser = new ConcurrentHashMap<>(); // userEmail -> orderId
    private final Map<String, String> tidByOrderId = new ConcurrentHashMap<>();  // orderId -> tid
    private final Map<String, Long> courseIdByOrderId = new ConcurrentHashMap<>(); // orderId -> courseId


    @Transactional
    public KakaoPayReadyResponseDto ready(Course course, String userEmail) {
        try {
   //         log.info(" [KakaoPay] Admin Key: {}", kakaoPaySecretKey);
   //         log.info(" [KakaoPay] CID: {}", cid);

            log.info(" course title = {}", course.getTitle());
            log.info(" course price = {}", course.getPrice());
            log.info(" userEmail = {}", userEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "SECRET_KEY " + kakaoPaySecretKey);
            log.info(" Authorization 헤더: {}", headers.getFirst("Authorization"));

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
                    //  카카오 → 프론트 라우트로 보냄
                    .approvalUrl(clientHost + "/payment/success")
                    .cancelUrl(clientHost + "/payment/cancel")
                    .failUrl(clientHost + "/payment/fail")
                    .build();

            HttpEntity<KakaoPayReadyRequestDto> entity = new HttpEntity<>(request, headers);

            //  우선 String으로 응답 받아서 로그 확인
            ResponseEntity<String> rawResponse = new RestTemplate().postForEntity(
                    "https://open-api.kakaopay.com/online/v1/payment/ready",
                    entity,
                    String.class
            );

            log.info(" [카카오 응답 원문] {}", rawResponse.getBody());

            // ✅ 응답을 DTO로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            KakaoPayReadyResponseDto res = objectMapper.readValue(rawResponse.getBody(),
                    KakaoPayReadyResponseDto.class);

            orderIdByUser.put(userEmail, orderId);
            tidByOrderId.put(orderId, res.getTid());
            courseIdByOrderId.put(orderId, course.getId());

//            userTidMap.put(userEmail, responseDto.getTid());
//            userOrderIdMap.put(userEmail, orderId);

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
    public String approve(String pgToken, String userEmail) {
        String orderId = orderIdByUser.get(userEmail);
        if (orderId == null) throw new IllegalStateException("주문정보가 없습니다. 다시 시도해 주세요.");

        String tid = tidByOrderId.get(orderId);
        if (tid == null) throw new IllegalStateException("TID가 없습니다. 다시 시도해 주세요.");

        Long courseId = courseIdByOrderId.get(orderId);
        if (courseId == null) throw new IllegalStateException("코스 정보가 없습니다. 다시 시도해 주세요.");

        // 카카오 승인
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + kakaoPaySecretKey);

        var req = KakaoPayApproveRequestDto.builder()
                .cid(cid).tid(tid).partnerOrderId(orderId)
                .partnerUserId(userEmail).pgToken(pgToken).build();

        var entity = new HttpEntity<>(req, headers);
        var response = new RestTemplate().postForEntity(
                "https://open-api.kakaopay.com/online/v1/payment/approve", entity, String.class);

        // 수강 등록 멱등
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자 없음"));
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalStateException("코스 없음"));

        boolean enrolled = userCourseRepository.existsByUserIdAndCourseId(user.getId(), course.getId());
        if (!enrolled) {
            userCourseRepository.save(
                    UserCourse.builder()
                            .user(user)
                            .course(course)
                            .registeredAt(java.time.LocalDateTime.now())
                            .build()
            );
        }

        // 임시 맵 정리
        tidByOrderId.remove(orderId);
        courseIdByOrderId.remove(orderId);
        orderIdByUser.remove(userEmail);

        return response.getBody();
    }


}



