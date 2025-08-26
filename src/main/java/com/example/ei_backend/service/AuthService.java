package com.example.ei_backend.service;

import com.example.ei_backend.aws.S3Uploader;
import com.example.ei_backend.domain.UserRole;
import com.example.ei_backend.domain.dto.CourseProgressDto;
import com.example.ei_backend.domain.dto.MyPageResponseDto;
import com.example.ei_backend.domain.dto.PaymentDto;
import com.example.ei_backend.domain.dto.UserDto;
import com.example.ei_backend.domain.dto.auth.LoginResult;
import com.example.ei_backend.domain.email.EmailSender;
import com.example.ei_backend.domain.entity.EmailVerification;
import com.example.ei_backend.domain.entity.ProfileImage;
import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.exception.CustomException;
import com.example.ei_backend.exception.ErrorCode;
import com.example.ei_backend.exception.NotFoundException;
import com.example.ei_backend.mapper.UserMapper;
import com.example.ei_backend.repository.EmailVerificationRepository;
import com.example.ei_backend.repository.PaymentRepository;
import com.example.ei_backend.repository.RefreshTokenRepository;
import com.example.ei_backend.repository.UserRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailSender emailSender;
    private final RefreshTokenRepository refreshTokenRepository;
    private final S3Uploader s3Uploader;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;

    @Value("${app.client.host}")
    private String clientHost;

    /** 회원가입 요청 */
    @Transactional
    public void requestSignup(UserDto.Request dto) {
        if (userRepository.existsByEmailAndIsDeletedFalse(dto.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String code = createRandomCode();

        // 비밀번호 암호화
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));

        // 요청 JSON 저장
        final String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.VERIFICATION_JSON_ERROR);
        }

        emailVerificationRepository.save(
                EmailVerification.builder()
                        .email(dto.getEmail())
                        .code(code)
                        .expirationTime(LocalDateTime.now().plusMinutes(15))
                        .isVerified(false)
                        .requestData(requestJson)
                        .build()
        );

        String verifyLink = clientHost + "/api/auth/verify?email=" + dto.getEmail() + "&code=" + code;
        String content = "아래 링크를 클릭하여 회원가입을 완료하세요:\n" + verifyLink;
        emailSender.send(dto.getEmail(), "DongCheol-Coding 회원가입 인증", content);
    }

    /** 이메일 + 코드로 최종 가입 */
    @Transactional
    public UserDto.Response verifyAndSignup(String email, String code) {
        // 1) 이메일+코드+만료안됨 으로 직접 조회 (오래된 링크/다중코드 이슈 제거)
        EmailVerification v = emailVerificationRepository
                .findByEmailAndCodeAndExpirationTimeAfter(email, code, LocalDateTime.now())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.INVALID_VERIFY_CODE, "코드가 없거나 만료되었습니다."));

        // 2) 이미 인증됐으면 종료
        if (v.isVerified()) {
            throw new CustomException(ErrorCode.CONFLICT, "이미 인증되었습니다.");
        }

        // 3) 가입 여부 최종 점검 (소셜/이전 가입 등)
        if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 4) 상태 변경 (내부에서 verified=true, verifiedAt 등)
        v.verify(code);

        // 5) 사용자 생성/저장
        UserDto.Request dto = extractRequestDto(v);
        User user = userMapper.toEntity(dto);   // dto.password는 requestSignup에서 이미 인코딩됨
        user.addRole(UserRole.ROLE_MEMBER);



        try {
            userRepository.save(user);
            userRepository.flush();   // 예외 즉시 표면화
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DATABASE_CONSTRAINT_VIOLATION, "저장 실패(중복/필수값 누락)");
        }

        // 6) 토큰 발급
        String token = jwtTokenProvider.generateAccessToken(
                user.getEmail(),
                user.getRoles().stream().map(Enum::name).toList()
        );
        return UserDto.Response.fromEntity(user, token);
    }


    /** 코드만으로 최종 가입 (링크 클릭 방식) */
    @Transactional
    public UserDto.Response verifyAndSignup(String code) {
        var v = emailVerificationRepository
                .findByCodeAndExpirationTimeAfter(code, LocalDateTime.now()) // 레포지토리 메서드 추가 필요
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFY_CODE, "코드가 없거나 만료되었습니다."));

        if (v.isVerified()) throw new CustomException(ErrorCode.CONFLICT, "이미 인증되었습니다.");
        if (userRepository.existsByEmailAndIsDeletedFalse(v.getEmail()))
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);

        v.verify(code);

        var dto  = extractRequestDto(v);
        var user = userMapper.toEntity(dto);   // ⚠️ 재인코딩 금지
        user.addRole(UserRole.ROLE_MEMBER);

        userRepository.saveAndFlush(user);

        String token = jwtTokenProvider.generateAccessToken(
                user.getEmail(), user.getRoles().stream().map(Enum::name).toList());
        return UserDto.Response.fromEntity(user, token);
    }

    /** EmailVerification -> UserDto.Request 변환 */
    private UserDto.Request extractRequestDto(EmailVerification verification) {
        try {
            return objectMapper.readValue(verification.getRequestData(), UserDto.Request.class);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.VERIFICATION_JSON_ERROR, "요청 직렬화에 실패했습니다.");
        }
    }

    /** 비밀번호 변경 */
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.validatePassword(newPassword, passwordEncoder);
    }

    @Transactional
    public void logout(String email, String refreshToken) {
        if (StringUtils.hasText(email)) {
            refreshTokenRepository.deleteByEmail(email);
            return;
        }
        if (StringUtils.hasText(refreshToken)) {
            refreshTokenRepository.deleteByToken(refreshToken);
        }

    }


    /** 회원 탈퇴 */
    @Transactional
    public void deleteAccount(Long userId) {
        deleteAccount(userId, null);
    }

    public void deleteAccount(Long userId, @Nullable String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        // 도메인 메서드로 소프트 삭제 + 마스킹(익명화)
        user.softDelete(reason);          // isDeleted=true, deletedAt=now, deletedReason 저장 등
        user.anonymizeSensitiveFields();  // 이메일/닉네임 마스킹 및 유니크 충돌 방지
        user.bumpTokenVersion();          // 이후의 액세스 토큰 검증에서 차단

        // Refresh Token 제거 (이메일 기준)
        refreshTokenRepository.deleteByEmail(user.getEmail());
    }

    /** 로그인 */
    @Transactional // ✅ readOnly=false
    public LoginResult login(String email, String password) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // (도메인 모델 패턴이라면 user.verifyPassword(...)로 캡슐화)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        var roles = user.getRoles().stream().map(Enum::name).toList();

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // ✅ RT 회전 저장 (email 기준 1개 유지)
        refreshTokenRepository.findByEmail(user.getEmail())
                .ifPresent(refreshTokenRepository::delete);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .email(user.getEmail())
                        .token(refreshToken)
                        .build()
        );

        return new LoginResult(user.getId(), user.getEmail(), roles, accessToken, refreshToken);
    }

    /** 관리자 검색 */
    @Transactional(readOnly = true)
    public List<User> searchUser(String name, String phoneSuffix, String role) {
        List<User> allUsers = userRepository.findAll(); // (아래 3번 스펙 방식 권장)
        Stream<User> stream = allUsers.stream();

        boolean hasName = name != null && !name.isBlank();
        boolean hasSuffix = phoneSuffix != null && !phoneSuffix.isBlank();
        boolean hasRole = role != null && !role.isBlank();

        if (hasName || hasSuffix) {
            stream = stream.filter(u -> u.matchesAny(name, phoneSuffix));
        }
        if (hasRole) {
            UserRole targetRole = parseRole(role);
            stream = stream.filter(u -> u.hasRole(targetRole));
        }
        return stream.toList();
    }

    @Transactional(readOnly = true)
    public Page<User> searchUserPage(String name, String phoneSuffix, String role, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            query.distinct(true);             //  roles join으로 생길 수 있는 중복 제거
            return cb.conjunction();          // always-true
        };

        if (name != null && !name.isBlank()) {
            String like = "%" + name.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (phoneSuffix != null && !phoneSuffix.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.like(root.get("phone"), "%" + phoneSuffix));
        }
        if (role != null && !role.isBlank()) {
            UserRole target = parseRole(role);
            spec = spec.and((root, q, cb) -> cb.equal(root.join("roles"), target)); // ElementCollection 조인
            // 대안: cb.isMember(target, root.get("roles"))
        }

        return userRepository.findAll(spec, pageable);
    }


    /** 프로필 이미지 업로드/교체 */
    @Transactional
    public String updateProfileImage(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String imageUrl = s3Uploader.upload(file, "profile-images");
        ProfileImage profileImage = user.getProfileImage();

        if (profileImage != null) {
            profileImage.updateImageUrl(imageUrl);
        } else {
            ProfileImage newImage = ProfileImage.builder()
                    .imageUrl(imageUrl)
                    .build();
            user.setProfileImage(newImage);
        }
        return imageUrl;
    }

    /** 프로필 이미지 삭제 */
    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ProfileImage profileImage = user.getProfileImage();
        if (profileImage == null) {
            throw new CustomException(ErrorCode.PROFILE_IMAGE_NOT_FOUND);
        }

        s3Uploader.delete(profileImage.getImageUrl());
        user.setProfileImage(null);
    }

    /** 마이페이지 묶음 조회 */
    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPageInfo(User user) {
        UserDto.Response userDto = userMapper.toResponse(user);

        var paymentDtos = paymentRepository.findApprovedByUserIdWithCourse(user.getId())
                .stream()
                .map(p -> PaymentDto.builder()
                        .courseId(p.getCourse().getId())
                        .courseName(p.getCourse().getTitle())
                        .price(p.getAmount())
                        .paymentDate(p.getPaymentDate())  // LocalDateTime 그대로
                        .build())
                .toList();

        // 진행도는 별도 로직이 없으니 일단 빈 리스트 유지
        List<CourseProgressDto> courseProgressDtos = List.of();

        return MyPageResponseDto.builder()
                .user(userDto)
                .payments(paymentDtos)
                .coursesProgress(courseProgressDtos)
                .build();
    }


    /** 문자열 역할 파싱 */
    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_ROLE);
        }
    }

    /** 회원가입 코드 생성 */
    private String createRandomCode() {
        int length = 6;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }

    @Transactional
    public void logout(String email) {
        refreshTokenRepository.findByEmail(email).ifPresent(refreshTokenRepository::delete);
    }
}
