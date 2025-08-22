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
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Order(20)
@Slf4j
public class TestPurchaseSeed implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("[Seed] 시작");

        // 1) 회원 upsert (코스 유무와 무관하게 항상 생성/유지)
        User member = upsertMember("member3@test.com", "결제회원3", "member123!");
        log.info("[Seed] member3 존재 확인: id={}", member.getId());

        // 2) 코스 upsert (없으면 생성해서 순서 의존 제거)
        Course course = upsertCourse("동철코딩 백엔드 강의", 990000,
                "실전 프로젝트 백엔드 강의",
                "https://my-project-bucket.s3.ap-northeast-2.amazonaws.com/sample.jpg");
        log.info("[Seed] course 존재 확인: id={}, title={}", course.getId(), course.getTitle());

        // 3) 결제 승인 내역 보장
        ensureApprovedPayment(member, course);
        // 4) 수강 등록 보장
        ensureEnrollment(member, course);

        log.info("✅ 테스트용 결제 완료 MEMBER + 수강 등록 시드 완료");
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

    private Course upsertCourse(String title, int price, String description, String imageUrl) {
        return courseRepository.findByTitle(title).orElseGet(() -> {
            Course c = Course.builder()
                    .title(title)
                    .description(description)
                    .price(price)
                    .imageUrl(imageUrl)
                    .build();
            return courseRepository.save(c);
        });
    }

    private void ensureApprovedPayment(User user, Course course) {
        // 중복 시드 방지: TID 기준으로 체크
        String seedTid = "SEED-TID-" + user.getId() + "-" + course.getId();
        if (paymentRepository.existsByTid(seedTid)) {
            log.info("[Seed] 이미 승인 결제가 존재합니다. (tid={})", seedTid);
            return;
        }

        Payment payment = Payment.builder()
                .user(user)
                .course(course)
                .orderId("SEED-ORDER-" + UUID.randomUUID())   // ★ NOT NULL + UNIQUE
                .tid(seedTid)                                  // ★ NOT NULL + UNIQUE
                .amount(course.getPrice())
                .method(PaymentMethod.KAKAOPAY)
                .status(PaymentStatus.APPROVED)
                .paymentDate(LocalDateTime.now())             // ★ NOT NULL
                .requestedAt(LocalDateTime.now().minusMinutes(1))
                .approvedAt(LocalDateTime.now())
                // .pgTid(seedTid)  // 선택: 굳이 필요 없으면 생략
                .build();

        paymentRepository.save(payment);
        log.info("[Seed] 승인 결제 생성 완료 (paymentId={})", payment.getId());
    }

    private void ensureEnrollment(User user, Course course) {
        boolean enrolled = userCourseRepository.existsByUserIdAndCourseId(user.getId(), course.getId());
        if (enrolled) {
            log.info("[Seed] 이미 수강 등록되어 있습니다. (userId={}, courseId={})", user.getId(), course.getId());
            return;
        }
        UserCourse uc = UserCourse.builder()
                .user(user)
                .course(course)
                .registeredAt(LocalDateTime.now())
                .build();
        userCourseRepository.save(uc);
        log.info("[Seed] 수강 등록 생성 완료 (userCourseId={})", uc.getId());
    }
}
