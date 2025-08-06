package com.example.ei_backend.service;

import com.example.ei_backend.aws.S3Uploader;
import com.example.ei_backend.domain.ErrorCode;
import com.example.ei_backend.domain.UserRole;
import com.example.ei_backend.domain.dto.UserDto;
import com.example.ei_backend.domain.email.EmailSender;
import com.example.ei_backend.domain.entity.EmailVerification;
import com.example.ei_backend.domain.entity.ProfileImage;
import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.exception.CustomException;
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


    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()) // LocalDate, LocalDateTime 직렬화 지원
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 날짜를 ISO 8601 형식으로 출력
    private final S3Uploader s3Uploader;

    @Value("${app.client.host}")
    private String clientHost;

    /**
     * 회원가입
     */
    @Transactional
    public void requestSignup(UserDto.Request dto) {
        if (userRepository.existsByEmailAndIsDeletedFalse(dto.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        String code = createRandomCode();

        // ✅ password 암호화
        String rawPassword = dto.getPassword();
        dto.setPassword(passwordEncoder.encode(rawPassword));

        // ✅ 안전하게 직렬화
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("회원가입 정보를 JSON으로 변환하는 데 실패했습니다.", e);
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

        @Transactional
         public UserDto.Response verifyAndSignup(String email, String code) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByExpirationTimeDesc(email)
                .orElseThrow(() -> new IllegalStateException("인증 요청 없음"));

        verification.verify(code); // 검증

        UserDto.Request dto = extractRequestDto(verification); // 직렬화 해제
        User user = userMapper.toEntity(dto); // password는 이미 암호화된 상태
        user.addRole(UserRole.ROLE_MEMBER);
        userRepository.save(user);

            String token = jwtTokenProvider.generateAccessToken(
                    user.getEmail(),
                    user.getRoles().stream().map(Enum::name).toList()
            );
        return UserDto.Response.fromEntity(user, token);
    }


    private UserDto.Request extractRequestDto(EmailVerification verification) {
        try {
            return objectMapper.readValue(verification.getRequestData(), UserDto.Request.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("회원가입 요청 데이터를 복원할 수 없습니다.", e);
        }
    }
    @Transactional
    public UserDto.Response verifyAndSignup(String code) {
        EmailVerification verification = emailVerificationRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 코드입니다."));

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
    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자"));
        user.changePassword(newPassword, passwordEncoder);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));
        user.deleteAccount();
    }

    /**
     * 로그인
     */
    @Transactional(readOnly = true)
    public UserDto.Response login(String email, String password) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalStateException("이메일 또는 비밀번호가 잘못되었습니다.");
        }
        String token = jwtTokenProvider.generateAccessToken(
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

        return UserDto.Response.fromEntity(user, token);
    }

    /**
     * 검색 로직 구현
     */
    @Transactional(readOnly = true)
    public List<User> searchUser(String name, String phoneSuffix, String role) {
        List<User> allUsers = userRepository.findAll();

        Stream<User> stream = allUsers.stream();

        // 이름 or 핸드폰 번호 조건
        if (name != null || phoneSuffix != null) {
            stream = stream.filter(user -> user.matchesAny(name, phoneSuffix));
        }
        // 역할 필터
        if (role != null) {
            UserRole targetRole = parseRole(role);
            stream = stream.filter(user -> user.hasRole(targetRole));
        }

        return stream.toList();

    }

    @Transactional
    public String updateProfileImage(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        String imageUrl = s3Uploader.upload(file, "profile-images");
        ProfileImage profileImage = user.getProfileImage();

        if (profileImage != null) {
            //  update 쿼리만 발생
            profileImage.updateImageUrl(imageUrl);
        } else {
            // 처음 등록인 경우 새로 생성
            ProfileImage newImage = ProfileImage.builder()
                    .imageUrl(imageUrl)
                    .build();

            user.setProfileImage(newImage); // 연관관계 설정
        }

        return imageUrl;
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(("사용자 없음")));

        ProfileImage profileImage = user.getProfileImage();
        if (profileImage == null) {
            throw new IllegalStateException("프로필 이미지 없음");
        }

        s3Uploader.delete(profileImage.getImageUrl());

        user.setProfileImage(null);
    }



    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_ROLE);
        }
    }

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
