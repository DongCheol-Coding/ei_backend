package com.example.ei_backend.testadmin;

import com.example.ei_backend.domain.UserRole;
import com.example.ei_backend.domain.entity.*;
import com.example.ei_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Order(25) // 코스 시드(@Order(5)) 이후 실행되도록 숫자 크게
@Slf4j
public class TestPurchaseSeed implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    // 테스트 회원 정보
    private static final String TEST_EMAIL = "member3@test.com";
    private static final String TEST_NAME  = "결제회원3";
    private static final String TEST_PW    = "member123!";

    // 이번에 “모두 결제 상태로 만들” 코스 타이틀
    private static final List<String> TARGET_TITLES = List.of(
            "DATA AI",
            "풀스택",
            "프론트엔드",
            "백엔드"
    );

    @Override
    public void run(String... args) {
        log.info("[Seed] 결제/수강 시드 시작");

        // 1) 회원 upsert
        User member = upsertMember(TEST_EMAIL, TEST_NAME, TEST_PW);
        log.info("[Seed] 테스트 회원 확인: id={}, email={}", member.getId(), member.getEmail());

        // 2) 대상 코스 조회 (TestDataInitializer가 upsert 했다고 가정)
        // 2) 대상 코스 조회 (TestDataInitializer가 upsert 했다고 가정)
        for (String title : TARGET_TITLES) {
            // ✅ 단일 결과 예외 방지: Top1 선택
            Optional<Course> oc = courseRepository.findFirstByTitleAndDeletedFalseOrderByIdAsc(title);
            // 공개된 것만 쓰려면 위 라인 대신 아래 라인 사용
            // Optional<Course> oc = courseRepository.findFirstByTitleAndPublishedTrueAndDeletedFalseOrderByIdAsc(title);

            if (oc.isEmpty()) {
                log.warn("[Seed] 코스를 찾지 못했습니다. title='{}' (TestDataInitializer 순서/DB 상태 확인 필요)", title);
                continue;
            }

            Course course = oc.get();
            log.info("[Seed] 코스 확인: id={}, title={}, price={}", course.getId(), course.getTitle(), course.getPrice());

            // 3) 결제 승인 내역 보장
            ensureApprovedPayment(member, course);

            // 4) 수강 등록 보장 (중복 방지)
            ensureEnrollment(member, course);
        }

        log.info("✅ 테스트 회원의 4개 코스 결제/수강 상태 보장 완료");
    }

    private User upsertMember(String email, String name, String rawPassword) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .name(name)
                    .isSocial(false)
                    .isDeleted(false)
                    .roles(Set.of(UserRole.ROLE_MEMBER))
                    .build();
            return userRepository.save(user);
        });
    }

    /** 해당 (user, course)에 대해 APPROVED 결제를 멱등 보장 */
    private void ensureApprovedPayment(User user, Course course) {
        // 멱등용 고정 TID (userId-courseId 조합)
        String seedTid = "SEED-TID-" + user.getId() + "-" + course.getId();
        if (paymentRepository.existsByTid(seedTid)) {
            log.info("[Seed] 이미 승인 결제가 존재합니다. tid={}", seedTid);
            return;
        }

        Payment payment = Payment.builder()
                .user(user)
                .course(course)
                .orderId("SEED-ORDER-" + UUID.randomUUID()) // 고유
                .tid(seedTid)                               // 고유/멱등
                .amount(course.getPrice())
                .method(PaymentMethod.KAKAOPAY)
                .status(PaymentStatus.APPROVED)
                .requestedAt(LocalDateTime.now().minusMinutes(1))
                .paymentDate(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        log.info("[Seed] 승인 결제 생성 완료: paymentId={}, tid={}", payment.getId(), seedTid);
    }

    /** user_courses의 UNIQUE(user_id, course_id) 제약 존중 + 멱등 보장 */
    private void ensureEnrollment(User user, Course course) {
        boolean exists = userCourseRepository.existsByUserIdAndCourseId(user.getId(), course.getId());
        if (exists) {
            log.info("[Seed] 이미 수강 등록되어 있음 (userId={}, courseId={})", user.getId(), course.getId());
            return;
        }
        UserCourse uc = UserCourse.builder()
                .user(user)
                .course(course)
                .registeredAt(LocalDateTime.now())
                .build();
        userCourseRepository.save(uc);
        log.info("[Seed] 수강 등록 생성 완료: userCourseId={}", uc.getId());
    }
}
