package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.KakaoPayApproveRequestDto;
import com.example.ei_backend.domain.dto.KakaoPayReadyResponseDto;
import com.example.ei_backend.domain.dto.PaymentDto;
import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.exception.ErrorCode;
import com.example.ei_backend.mapper.PaymentMapper;
import com.example.ei_backend.repository.PaymentRepository;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.CourseService;
import com.example.ei_backend.service.KakaoPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payment", description = "카카오페이 결제 준비/승인 및 취소/실패 콜백")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
@SecurityRequirement(name = "accessTokenCookie") // 쿠키 AT 인증 사용 시
// @SecurityRequirement(name = "bearerAuth")     // Bearer 사용 시 교체
public class PaymentController {

    private final KakaoPayService kakaoPayService;
    private final CourseService courseService;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    /** 1) 결제 준비 */
    @Operation(
            summary = "결제 준비",
            description = "코스 ID로 카카오페이 결제 준비를 수행하고, 프론트가 이동할 리다이렉트 URL을 반환합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(
                                    name = "ready-success",
                                    description = "카카오 결제 페이지로 이동할 URL",
                                    value = "\"https://mockup-pg-web.kakao.com/v1/xxxxxxxxxxxxxxxx\""
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청/코스 ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코스 없음")
    })
    @PostMapping("/ready")
    public ResponseEntity<ApiResponse<String>> createPayment(
            @Parameter(description = "결제할 코스 ID", example = "1")
            @RequestParam Long courseId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String userEmail = userPrincipal.getUsername();
        Course course = courseService.findById(courseId); // 코스 조회 실패 시 예외 처리 가정
        KakaoPayReadyResponseDto ready = kakaoPayService.ready(course, userEmail);
        return ResponseEntity.ok(ApiResponse.ok(ready.getNextRedirectPcUrl()));
    }

    /** 2) 결제 승인 (카카오 콜백) */
    @Operation(
            summary = "결제 승인 콜백",
            description = "카카오에서 리다이렉트된 후 제공되는 pg_token으로 결제를 승인합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "승인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(
                                    name = "approve-success",
                                    value = "\"결제가 완료되었습니다.\""
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "pg_token 누락/형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "402", description = "결제 승인 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })

    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<String>> approvePayment(
            @RequestParam("pg_token") String pgToken,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        String result = kakaoPayService.approve(pgToken, me.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("결제가 완료되었습니다."));
    }


    /** 3) 결제 취소 */
    @Operation(
            summary = "결제 취소 콜백",
            description = "사용자가 카카오 결제 페이지에서 취소할 경우 호출됩니다. (인증 불필요)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "취소 처리",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "cancel",
                                    value = """
                    {
                      "success": false,
                      "code": "PAYMENT_FAILED",
                      "status": 400,
                      "message": "결제가 취소되었습니다.",
                      "data": null
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/cancel")
    @SecurityRequirement(name = "") // 공개 엔드포인트로 문서화 (전역 보안 요구 무시)
    public ResponseEntity<ApiResponse<Void>> cancelPayment() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PAYMENT_FAILED, "결제가 취소되었습니다."));
    }

    /** 4) 결제 실패 */
    @Operation(
            summary = "결제 실패 콜백",
            description = "카카오 결제 진행 중 오류가 발생한 경우 호출됩니다. (인증 불필요)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500", description = "실패 처리",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "fail",
                                    value = """
                    {
                      "success": false,
                      "code": "SERVER_ERROR",
                      "status": 500,
                      "message": "결제에 실패하였습니다.",
                      "data": null
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/fail")
    @SecurityRequirement(name = "") // 공개 엔드포인트로 문서화 (전역 보안 요구 무시)
    public ResponseEntity<ApiResponse<Void>> failPayment() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.SERVER_ERROR, "결제에 실패하였습니다."));
    }

    /** 5) 내 결제 내역 조회 */
    @Operation(
            summary = "내 결제 내역 조회",
            description = "로그인한 사용자의 결제 내역을 최신순으로 반환합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getMyPayments(
            @AuthenticationPrincipal UserPrincipal me
    ) {
        var payments = paymentRepository.findByUserIdOrderByIdDesc(me.getUserId());
        var dtoList = payments.stream()
                .map(paymentMapper::toDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(dtoList));
    }
}
