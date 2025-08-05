package com.example.ei_backend.controller;

import com.example.ei_backend.domain.dto.CourseDto;
import com.example.ei_backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class CourseController {

    private final CourseService courseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseDto.Response> createProduct(
            @ModelAttribute CourseDto.Request request
    ) throws IOException {
        CourseDto.Response response = courseService.createProduct(request);
        return ResponseEntity.ok(response);
    }
}
