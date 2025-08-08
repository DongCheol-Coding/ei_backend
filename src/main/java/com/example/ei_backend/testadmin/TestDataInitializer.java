package com.example.ei_backend.testadmin;

import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TestDataInitializer {

    private final CourseRepository courseRepository;

    @Bean
    public CommandLineRunner initCourseData() {
        return args -> {
            if (courseRepository.count() == 0) {
                Course course = Course.builder()
                        .title("동철코딩 백엔드 강의")
                        .description("실전 프로젝트 백엔드 강의")
                        .price(100)
                        .imageUrl("https://my-project-bucket.s3.ap-northeast-2.amazonaws.com/sample.jpg")
                        .build();

                courseRepository.save(course);
                System.out.println("✅ 테스트용 Course 등록 완료");
            }
        };
    }
}
