package com.example.ei_backend.testadmin;

import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class TestDataInitializer {

    private final CourseRepository courseRepository;

    @Bean
    @Order(5) // 다른 시드보다 먼저/나중에 실행하고 싶으면 숫자 조절
    public CommandLineRunner initCourseData() {
        return args -> {
            upsertCourse(
                    "DATA AI",
                    "데이터 분석 · 머신러닝 · MLOps 핵심 커리큘럼",
                    219,
                    cdn("https://my-project-bucket-8655.s3.ap-northeast-2.amazonaws.com/product-images/2c774c9f-a4c6-4563-acdc-24d15ff1ada2_001.png")
            );
            upsertCourse(
                    "풀스택",
                    "프론트엔드 + 백엔드 풀스택 실전 프로젝트",
                    259,
                    cdn("https://my-project-bucket-8655.s3.ap-northeast-2.amazonaws.com/product-images/4a140299-9719-4da4-a15d-5a8d58b2d071_002.png")
            );
            upsertCourse(
                    "프론트엔드",
                    "React 기반 웹 프론트엔드 심화",
                    179,
                    cdn("https://my-project-bucket-8655.s3.ap-northeast-2.amazonaws.com/product-images/6db90e51-6a12-4ffb-af91-1f2a5267321d_004.png")
            );
            upsertCourse(
                    "백엔드",
                    "Java/Spring Boot 백엔드 핵심 & 실전",
                    199,
                    cdn("https://my-project-bucket-8655.s3.ap-northeast-2.amazonaws.com/product-images/fb767e57-4572-4b81-b30b-9eec00e2af5b_003.png")
            );
            System.out.println("▶ 코스 시드 완료 (필요한 항목만 upsert)");
        };
    }

    private void upsertCourse(String title, String description, int price, String imageUrl) {
        Optional<Course> exists = courseRepository.findByTitle(title);
        if (exists.isPresent()) return;

        courseRepository.save(
                Course.builder()
                        .title(title)
                        .description(description)
                        .price(price)
                        .imageUrl(imageUrl)
                        .published(true)
                        .build()
        );
    }

    private static String cdn(String path) {
        return "https://cdn.dongcheolcoding.life" + path;
    }
}
