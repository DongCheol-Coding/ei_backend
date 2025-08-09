package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.CourseDto;
import com.example.ei_backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course")
public class CourseController {

    private final CourseService courseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')") // hasRole은 내부에서 ROLE_ 접두사 붙음
    public ResponseEntity<ApiResponse<CourseDto.Response>> createProduct(
            @ModelAttribute CourseDto.Request request
    ) throws IOException {
        CourseDto.Response response = courseService.createProduct(request);
        // 생성이므로 201로 응답
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }
}
