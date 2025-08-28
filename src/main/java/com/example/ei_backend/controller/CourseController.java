package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.CourseDto;
import com.example.ei_backend.domain.dto.CourseProgressDto;
import com.example.ei_backend.domain.dto.CoursePurchasePreviewDto;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.CourseProgressService;
import com.example.ei_backend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Tag(name = "Course", description = "강의(코스) 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course")
@SecurityRequirement(name = "accessTokenCookie") // 쿠키 AT 인증 사용 시
// @SecurityRequirement(name = "bearerAuth")     // Bearer 사용 시 교체
public class CourseController {

    private final CourseService courseService;
    private final CourseProgressService courseProgressService;

    @Value("${app.progress.complete-threshold:90.0}")
    private double completeThreshold;

    @GetMapping("/{courseId}/progress")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "코스 진행률 조회",
            description = "사용자별 코스 진행률과 완료/전체 강의 수를 반환합니다.",
            security = { @SecurityRequirement(name = "accessTokenCookie") }
    )
    public ApiResponse<CourseProgressDto> getCourseProgress(
            @Parameter(hidden = true)
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "코스 ID", example = "101")
            @PathVariable Long courseId
    ) {
        Long userId = principal.getUserId();
        double percent = courseProgressService.getCourseProgressPercent(userId, courseId);
        var cnt = courseProgressService.getProgressCount(userId, courseId);
        return ApiResponse.ok(CourseProgressDto.of(percent, cnt.completedLectures(), cnt.totalLectures(), completeThreshold));
    }

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
                    schema = @Schema(implementation = CourseDto.CreateRequest.class)
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')") // 내부에서 ROLE_ 접두사 자동 부여
    public ResponseEntity<ApiResponse<CourseDto.Response>> createProduct(
            @ModelAttribute CourseDto.CreateRequest request
    ) throws IOException {
        var response = courseService.createProduct(request);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "코스 목록 조회(공개)", description = "발행된 코스를 전체 반환")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    public ResponseEntity<ApiResponse<List<CourseDto.Summary>>> listCourses() {
        var items = courseService.listAllPublicCourses();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/{courseId}/preview")
    @PreAuthorize("permitAll()")
    @Operation(summary = "코스 결제 미리보기(공개 전용)", description = "결제 직전에 노출할 코스 정보")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 미공개/삭제")
    })
    public ResponseEntity<ApiResponse<CoursePurchasePreviewDto>> getCoursePreview(
            @PathVariable Long courseId) {
        var dto = courseService.getPurchasePreview(courseId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    // 전체 공개 코스 목록 (검색/페이징)
//    @GetMapping
//    @PreAuthorize("permitAll()") // 공개로 열려면 추가 (혹은 시큐리티 설정에서 permitAll 처리)
//    @Operation(summary = "코스 목록 조회(공개)", description = "검색/페이징이 가능한 공개 코스 목록")
//    @ApiResponses({
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
//    })
//    public ResponseEntity<ApiResponse<CourseDto.Page<CourseDto.Summary>>> listCourses(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "12") int size,
//            @RequestParam(required = false) String q,
//            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
//    ) {
//        var items = courseService.findPublicCourses(page, size, q, sort);
//        return ResponseEntity.ok(ApiResponse.ok(items));
//    }

    // 공개/비공개 토글 (ADMIN)
    @PatchMapping("/{courseId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "코스 공개 상태 변경(ADMIN)", description = "published=true/false 토글")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않음")
    })
    public ResponseEntity<ApiResponse<CourseDto.Summary>> setPublished(
            @PathVariable Long courseId,
            @org.springframework.web.bind.annotation.RequestBody CourseDto.PublishRequest req
    ) {
        var updated = courseService.setPublished(courseId, req.isPublished());
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // 내 코스 목록 (수강 중)
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 코스 목록", description = "수강 중인 코스와 진행률")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<CourseDto.Page<CourseDto.MyCourseItem>>> myCourses(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.ei_backend.security.UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        var items = courseService.findMyCourses(principal.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }
}
