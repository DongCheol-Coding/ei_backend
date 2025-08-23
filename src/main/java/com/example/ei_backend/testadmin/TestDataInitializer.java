package com.example.ei_backend.testadmin;

import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TestDataInitializer {

    private final CourseRepository courseRepository;

    @Bean
    public CommandLineRunner initCourseData() {
        return args -> {
            // 이미 데이터가 있으면 아무 것도 안 함 (중복 insert 방지)
            if (courseRepository.count() > 0) return;

            List<Course> courses = List.of(
                    Course.builder()
                            .title("DATA AI")
                            .description("데이터 분석 · 머신러닝 · MLOps 핵심 커리큘럼")
                            .price(219)
                            .imageUrl(cdn("/images/courses/data-ai.jpg"))
                            .build(),
                    Course.builder()
                            .title("풀스택")
                            .description("프론트엔드 + 백엔드 풀스택 실전 프로젝트")
                            .price(259)
                            .imageUrl(cdn("/images/courses/fullstack.jpg"))
                            .build(),
                    Course.builder()
                            .title("프론트엔드")
                            .description("React 기반 웹 프론트엔드 심화")
                            .price(179)
                            .imageUrl(cdn("/images/courses/frontend.jpg"))
                            .build(),
                    Course.builder()
                            .title("백엔드")
                            .description("Java/Spring Boot 백엔드 핵심 & 실전")
                            .price(199)
                            .imageUrl(cdn("/images/courses/backend.jpg"))
                            .build()
            );

            courseRepository.saveAll(courses);
            System.out.println("▶ 초기 코스 4개 등록 완료 (DATA AI / 풀스택 / 프론트엔드 / 백엔드)");
        };
    }

    private static String cdn(String path) {
        // CDN을 쓰지 않으면 S3 경로로 바꿔도 됩니다.
        return "https://cdn.dongcheolcoding.life" + path;
    }
}
