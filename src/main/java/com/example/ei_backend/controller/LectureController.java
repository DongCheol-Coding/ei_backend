package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.lecture.*;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.LectureCommandService;
import com.example.ei_backend.service.LectureCreateWithVideoService;
import com.example.ei_backend.service.LectureQueryService;
import com.example.ei_backend.service.ProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Lecture", description = "강의 생성/수정/삭제, 조회, 진행도 업데이트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@SecurityRequirement(name = "accessTokenCookie") // 쿠키 AT 인증
// @SecurityRequirement(name = "bearerAuth")     // Bearer 사용 시 교체
public class LectureController {

    private final LectureCommandService lectureCommandService;
    private final LectureQueryService lectureQueryService;
    private final ProgressService progressService;
    private final LectureCreateWithVideoService createWithVideoService;
    private final ObjectMapper objectMapper;

    /** ADMIN: 멀티파트(강의 + 영상 한번에) */
    @Operation(summary = "강의 생성(영상 포함, multipart)",
            description = "관리자가 멀티파트로 강의 메타데이터와 영상을 함께 업로드하여 생성합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "생성 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LectureDetailDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(ADMIN)")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/courses/{courseId}/lectures/with-video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LectureDetailDto> createWithVideo(
            @Parameter(description = "코스 ID", example = "101") @PathVariable Long courseId,

            // 🔽 여기만 변경: LectureCreateRequest -> String
            @Parameter(
                    name = "data",
                    description = "강의 생성 JSON (LectureCreateRequest)",
                    required = true
            )
            @RequestPart("data") String dataJson,

            @Parameter(
                    name = "video",
                    description = "업로드할 강의 영상 파일(선택)",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart(value = "video", required = false) MultipartFile video
    ) throws Exception {
        // 문자열을 DTO로 역직렬화
        LectureCreateRequest lectureCreateRequest =
                objectMapper.readValue(dataJson, LectureCreateRequest.class);

        return ApiResponse.ok(
                createWithVideoService.create(courseId, lectureCreateRequest, video)
        );
    }


    @Operation(
            summary = "강의 수정(영상 포함, multipart)",
            description = "관리자가 멀티파트로 강의 메타데이터(JSON 문자열)와 영상을 함께 업로드하여 수정합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "수정 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LectureDetailDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(ADMIN)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강의 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/lectures/{lectureId}/with-video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LectureDetailDto> updateWithVideo(
            @Parameter(description = "강의 ID", example = "1001") @PathVariable Long lectureId,
            @Parameter(
                    name = "data",
                    description = "강의 수정 JSON(LectureUpdateRequest) 문자열",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LectureUpdateRequest.class))
            )
            @RequestPart("data") String dataJson,
            @Parameter(
                    name = "video",
                    description = "업로드할 강의 영상 파일(선택)",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart(value = "video", required = false) MultipartFile video
    ) throws Exception {
        LectureUpdateRequest req = objectMapper.readValue(dataJson, LectureUpdateRequest.class);
        return ApiResponse.ok(createWithVideoService.update(lectureId, req, video));
    }

    @Operation(summary = "강의 삭제", description = "관리자가 특정 강의를 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(ADMIN)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강의 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/lectures/{lectureId}")
    public ApiResponse<Void> delete(
            @Parameter(description = "강의 ID", example = "1001") @PathVariable Long lectureId
    ) {
        lectureCommandService.delete(lectureId);
        return ApiResponse.ok(null);
    }

    /** MEMBER/ADMIN 조회 */
    @Operation(summary = "강의 목록 조회", description = "사용자 권한에 맞는 코스의 강의 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LectureSummaryDto.class)))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/courses/{courseId}/lectures")
    public ApiResponse<List<LectureSummaryDto>> list(
            @AuthenticationPrincipal UserPrincipal me,
            @Parameter(description = "코스 ID", example = "101") @PathVariable Long courseId
    ) {
        return ApiResponse.ok(lectureQueryService.listForUser(me.getUserId(), courseId));
    }

    @Operation(summary = "강의 상세 조회", description = "사용자 권한에 맞는 강의 상세 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = LectureDetailDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강의 없음")
    })
    @GetMapping("/lectures/{lectureId}")
    public ApiResponse<LectureDetailDto> get(
            @AuthenticationPrincipal UserPrincipal me,
            @Parameter(description = "강의 ID", example = "1001") @PathVariable Long lectureId
    ) {
        return ApiResponse.ok(lectureQueryService.getForUser(me.getUserId(), lectureId));
    }

    @Operation(summary = "강의 진행도 업데이트", description = "시청한 초(watchedSec)를 업데이트하고 코스 진행 상태를 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CourseProgressWithLectureDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강의 없음")
    })
    @PostMapping("/lectures/{lectureId}/progress")
    public ApiResponse<CourseProgressWithLectureDto> progress(
            @AuthenticationPrincipal UserPrincipal me,
            @Parameter(description = "강의 ID", example = "1001") @PathVariable Long lectureId,
            @RequestBody(
                    required = true,
                    description = "진행도 업데이트 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProgressUpdateRequest.class))
            )
            @org.springframework.web.bind.annotation.RequestBody ProgressUpdateRequest req
    ) {
        return ApiResponse.ok(progressService.update(me.getUserId(), lectureId, req.getWatchedSec()));
    }

    // 테스트 용
//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping(value="/courses/{courseId}/lectures/with-video",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ApiResponse<LectureDetailDto> createWithVideo(
//            @PathVariable Long courseId,
//            @RequestPart("data") String dataJson,
//            @RequestPart(value="video", required=false) MultipartFile video
//    ) throws Exception {
//        LectureCreateRequest req =
//                objectMapper.readValue(dataJson, LectureCreateRequest.class);
//        return ApiResponse.ok(createWithVideoService.create(courseId, req, video));
//    }

}
