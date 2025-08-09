package com.example.ei_backend.service;

import com.example.ei_backend.aws.S3Uploader;
import com.example.ei_backend.domain.UserRole;
import com.example.ei_backend.domain.dto.CourseProgressDto;
import com.example.ei_backend.domain.dto.MyPageResponseDto;
import com.example.ei_backend.domain.dto.PaymentDto;
import com.example.ei_backend.domain.dto.UserDto;
import com.example.ei_backend.domain.email.EmailSender;
import com.example.ei_backend.domain.entity.EmailVerification;
import com.example.ei_backend.domain.entity.ProfileImage;
import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.exception.CustomException;
import com.example.ei_backend.exception.ErrorCode;
import com.example.ei_backend.mapper.UserMapper;
import com.example.ei_backend.repository.EmailVerificationRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByExpirationTimeDesc(email)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.VERIFICATION_NOT_FOUND,
                        "존재하지 않은 코드입니다."
                ));

        // 이미 인증된 경우
        if (verification.isVerified()) {
            throw new CustomException(ErrorCode.CONFLICT, "이미 인증되었습니다.");
        }

        // 코드 불일치
        if (!verification.getCode().equals(code)) {
            throw new CustomException(ErrorCode.INVALID_VERIFY_CODE, "존재하지 않은 코드입니다.");
        }

        // verify() 내부에서 상태 변경 + 예외 처리
        verification.verify(code);

        UserDto.Request dto = extractRequestDto(verification);
        User user = userMapper.toEntity(dto); // password는 이미 암호화 상태
        user.addRole(UserRole.ROLE_MEMBER);
        userRepository.save(user);

        String token = jwtTokenProvider.generateAccessToken(
                user.getEmail(),
                user.getRoles().stream().map(Enum::name).toList()
        );
        return UserDto.Response.fromEntity(user, token);
    }

    /** 코드만으로 최종 가입 (링크 클릭 방식) */
    @Transactional
    public UserDto.Response verifyAndSignup(String code) {
        EmailVerification verification = emailVerificationRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.INVALID_VERIFY_CODE,
                        "존재하지 않은 코드입니다."
                ));

        // 이미 인증된 경우
        if (verification.isVerified()) {
            throw new CustomException(ErrorCode.CONFLICT, "이미 인증되었습니다.");
        }

        // 코드 불일치 (혹시라도)
        if (!verification.getCode().equals(code)) {
            throw new CustomException(ErrorCode.INVALID_VERIFY_CODE, "존재하지 않은 코드입니다.");
        }

        verification.verify(code);

        UserDto.Request dto = extractRequestDto(verification);
        User user = userMapper.toEntity(dto);
        user.encodePassword(passwordEncoder.encode(user.getPassword()));
        user.addRole(UserRole.ROLE_MEMBER);
        userRepository.save(user);

        String token = jwtTokenProvider.generateAccessToken(
                user.getEmail(),
                user.getRoles().stream().map(Enum::name).toList()
        );
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


    /** 회원 탈퇴 */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.deleteAccount();
    }

    /** 로그인 */
    @Transactional(readOnly = true)
    public UserDto.Response login(String email, String password) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getEmail(),
                user.getRoles().stream().map(Enum::name).toList()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .email(user.getEmail())
                        .token(refreshToken)
                        .build()
        );

        return UserDto.Response.fromEntity(user, accessToken);
    }

    /** 관리자 검색 */
    @Transactional(readOnly = true)
    public List<User> searchUser(String name, String phoneSuffix, String role) {
        List<User> allUsers = userRepository.findAll();
        Stream<User> stream = allUsers.stream();

        if (name != null || phoneSuffix != null) {
            stream = stream.filter(user -> user.matchesAny(name, phoneSuffix));
        }
        if (role != null) {
            UserRole targetRole = parseRole(role);
            stream = stream.filter(user -> user.hasRole(targetRole));
        }
        return stream.toList();
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
    public MyPageResponseDto getMyPageInfo(User user) {
        UserDto.Response userDto = userMapper.toResponse(user);
        List<PaymentDto> paymentDtos = new ArrayList<>();
        List<CourseProgressDto> courseProgressDtos = new ArrayList<>();

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
}
