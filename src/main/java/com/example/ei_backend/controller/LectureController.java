package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.lecture.*;
import com.example.ei_backend.domain.entity.Lecture;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.LectureCommandService;
import com.example.ei_backend.service.LectureCreateWithVideoService;
import com.example.ei_backend.service.LectureQueryService;
import com.example.ei_backend.service.ProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class LectureController {

    private final LectureCommandService lectureCommandService;
    private final LectureQueryService lectureQueryService;
    private final ProgressService progressService;
    private final LectureCreateWithVideoService createWithVideoService;
    private final ObjectMapper objectMapper;

    // ADMIN: JSON만 (영상 없이)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/courses/{courseId}/lectures")
    public ApiResponse<LectureDto> create(@PathVariable Long courseId,
                                          @RequestBody LectureCreateRequest lectureCreateRequest) {
        return ApiResponse.ok(lectureCommandService.create(courseId, lectureCreateRequest));
    }

    // ADMIN: 멀티파트(강의 + 영상 한번에)
//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping(value = "/courses/{courseId}/lectures:with-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ApiResponse<LectureDetailDto> createWithVideo(@PathVariable Long courseId,
//                                                         @RequestPart("data") LectureCreateRequest lectureCreateRequest,
//                                                         @RequestPart(value = "video", required = false) MultipartFile video) {
//        return ApiResponse.ok(createWithVideoService.create(courseId, lectureCreateRequest, video));
//    }

    // 테스트 용
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value="/courses/{courseId}/lectures/with-video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LectureDetailDto> createWithVideo(
            @PathVariable Long courseId,
            @RequestPart("data") String dataJson,
            @RequestPart(value="video", required=false) MultipartFile video
    ) throws Exception {
        LectureCreateRequest req =
                objectMapper.readValue(dataJson, LectureCreateRequest.class);
        return ApiResponse.ok(createWithVideoService.create(courseId, req, video));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value="/lectures/{lectureId}/with-video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LectureDetailDto> updateWithVideo(
            @PathVariable Long lectureId,
            @RequestPart("data") String dataJson,
            @RequestPart(value="video", required=false) MultipartFile video
    ) throws Exception {
        LectureUpdateRequest req = objectMapper.readValue(dataJson, LectureUpdateRequest.class);
        return ApiResponse.ok(createWithVideoService.update(lectureId, req, video));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/lectures/{lectureId}")
    public ApiResponse<Void> delete(@PathVariable Long lectureId) {
        lectureCommandService.delete(lectureId);
        return ApiResponse.ok(null);
    }
    // MEMBER/ADMIN 조회
    @GetMapping("/courses/{courseId}/lectures")
    public ApiResponse<List<LectureSummaryDto>> list(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable Long courseId) {
        return ApiResponse.ok(lectureQueryService.listForUser(me.getUserId(), courseId));
    }

    @GetMapping("/lectures/{lectureId}")
    public ApiResponse<LectureDetailDto> get(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable Long lectureId) {
        return ApiResponse.ok(lectureQueryService.getForUser(me.getUserId(), lectureId));
    }

    @PostMapping("/lectures/{lectureId}/progress")
    public ApiResponse<CourseProgressWithLectureDto> progress(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable Long lectureId,
            @RequestBody ProgressUpdateRequest req) {
        return ApiResponse.ok(progressService.update(me.getUserId(), lectureId, req.getWatchedSec()));
    }


}


