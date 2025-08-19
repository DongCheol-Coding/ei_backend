package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.CourseDto;
import com.example.ei_backend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "Course", description = "강의(코스) 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course")
@SecurityRequirement(name = "accessTokenCookie") // 쿠키 AT 인증 사용 시
// @SecurityRequirement(name = "bearerAuth")     // Bearer 사용 시 교체
public class CourseController {

    private final CourseService courseService;

    @Operation(
            summary = "코스 생성",
            description = "멀티파트 폼으로 코스를 생성합니다. (예: 텍스트 필드 + 이미지/파일 업로드)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "생성 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CourseDto.Response.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(ADMIN만 가능)")
    })
    @RequestBody( // Swagger(OpenAPI)용 요청 바디 설명 (스프링의 @RequestBody 아님)
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CourseDto.Request.class)
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')") // 내부에서 ROLE_ 접두사 자동 부여
    public ResponseEntity<ApiResponse<CourseDto.Response>> createProduct(
            @ModelAttribute CourseDto.Request request
    ) throws IOException {
        CourseDto.Response response = courseService.createProduct(request);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }
}
