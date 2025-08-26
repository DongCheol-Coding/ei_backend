package com.example.ei_backend.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class CourseDto {

    @Getter @Setter
    @Schema(name = "CourseCreateRequest", description = "코스 생성 요청")
    public static class CreateRequest {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private String title;
        private String description;
        private Integer price;

        // 파일은 이렇게 표기해야 Swagger에서 업로드 필드로 보입니다
        @Schema(type = "string", format = "binary", description = "코스 이미지 파일")
        private MultipartFile image;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private int price;
        private String imageUrl;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Summary {
        private Long id;
        private String title;
        private String imageUrl;
        private Integer price;
        private boolean published;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PublishRequest {
        private boolean published;
        public boolean isPublished() { return published; }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyCourseItem {
        private Long courseId;
        private String courseTitle;
        private String imageUrl;
        private CourseProgressDto progress;
    }

    // 심플한 페이지 래퍼 (ApiResponse 안에 또 Page 쓰는 게 싫다면 이렇게)
    @Getter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Page<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
