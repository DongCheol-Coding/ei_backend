package com.example.ei_backend.domain.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

public class CourseDto {

    @Getter @Setter
    public static class Request {
        private String name;
        private String description;
        private MultipartFile image;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String imageUrl;
    }
}
