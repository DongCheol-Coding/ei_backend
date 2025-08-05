package com.example.ei_backend.service;

import com.example.ei_backend.aws.S3Uploader;
import com.example.ei_backend.domain.dto.CourseDto;
import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public CourseDto.Response createProduct(CourseDto.Request request) throws IOException {
        // 1. 이미지 업로드
        String imageUrl = s3Uploader.upload(request.getImage(), "product-images");

        // 2. 엔티티 생성
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(imageUrl)
                .build();

        courseRepository.save(course);

        // 3. 응답 DTO 반환
        return CourseDto.Response.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .imageUrl(course.getImageUrl())
                .build();
    }

}
