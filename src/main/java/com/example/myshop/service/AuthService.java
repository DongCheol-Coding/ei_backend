package com.example.myshop.service;

import com.example.myshop.domain.UserRole;
import com.example.myshop.domain.dto.UserDto;
import com.example.myshop.domain.email.EmailSender;
import com.example.myshop.domain.entity.EmailVerification;
import com.example.myshop.domain.entity.User;
import com.example.myshop.mapper.UserMapper;
import com.example.myshop.repository.EmailVerificationRepository;
import com.example.myshop.repository.UserRepository;
import com.example.myshop.security.JwtTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;

    /**
     * 회원가입
     */
    @Transactional
    public void requestSignup(UserDto.Request dto) {
        if (userRepository.existsByEmailAndIsDeletedFalse(dto.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        String code = createRandomCode();

        ObjectMapper objectMapper = new ObjectMapper();
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
                        .requestData(requestJson)  // 💡 dto 저장
                        .build()
        );

        String verifyLink = "http://localhost:8080/api/auth/verify?code=" + code;
        String content = "아래 링크를 클릭하여 회원가입을 완료하세요:\n" + verifyLink;
        emailSender.send(dto.getEmail(), "MyShop 회원가입 인증", content);
    }

        @Transactional
         public UserDto.Response verifyAndSignup(String email, String code) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByExpirationTimeDesc(email)
                .orElseThrow(() -> new IllegalStateException("인증 요청 없음"));

        verification.verify(code); // 검증

        UserDto.Request dto = extractRequestDto(verification); // 직렬화 해제
        User user = userMapper.toEntity(dto);
        user.encodePassword(passwordEncoder.encode(user.getPassword()));
        user.addRole(UserRole.BUYER);
        userRepository.save(user);

        String token = jwtTokenProvider.createToken(user.getEmail());
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

        verification.verify(code); // 도메인 로직

        UserDto.Request dto = extractRequestDto(verification); // 여기서 사용

        User user = userMapper.toEntity(dto);
        user.encodePassword(passwordEncoder.encode(user.getPassword()));
        user.addRole(UserRole.BUYER);
        userRepository.save(user);

        String token = jwtTokenProvider.createToken(user.getEmail());
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
        String token = jwtTokenProvider.createToken(user.getEmail());

        return UserDto.Response.fromEntity(user, token);
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

    @Transactional
    public UserDto.Response verifyCodeAndSignup(String email, String inputCode, UserDto.Request dto) {
        // 1. 인증 내역 확인
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByExpirationTimeDesc(email)
                .orElseThrow(() -> new IllegalStateException("인증 요청 내역이 없습니다."));

        // 2. 인증 코드 검증 및 상태 변경
        verification.verify(inputCode);

        // 3. 회원가입 완료
        if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        User user = userMapper.toEntity(dto);
        user.encodePassword(passwordEncoder.encode(user.getPassword()));
        user.addRole(UserRole.BUYER);
        userRepository.save(user);

        return userMapper.toResponse(user);
    }

    public void sendVerificationCode(String email) {
        // 1. 인증 코드 생성
        String code = createRandomCode();

        // 2. 인증 정보 저장 (DB)
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
        emailVerificationRepository.save(verification);

        // 3. 이메일 전송
        emailSender.send(email, "[MyShop] 인증 코드입니다", "인증 코드: " + code);
    }
    @Transactional
    public void verifyCode(String email, String inputCode) {
        EmailVerification verification = emailVerificationRepository.findTopByEmailOrderByExpirationTimeDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 내역이 없습니다."));

        verification.verify(inputCode); // 도메인 모델의 검증 로직 호출
    }

    @Transactional
    public void verifyEmail(String email, String inputCode) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByExpirationTimeDesc(email)
                .orElseThrow(() -> new IllegalStateException("인증 요청 내역이 없습니다."));

        verification.verify(inputCode);  // 도메인 메서드로 상태 변경
    }

    @Transactional
    public UserDto.Response completeSignup(UserDto.Request requestDto) {
        // 인증 여부 확인
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByExpirationTimeDesc(requestDto.getEmail())
                .orElseThrow(() -> new IllegalStateException("인증 요청 내역이 없습니다."));

        if (!verification.isVerified()) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        // 회원 정보 저장
        User user = userMapper.toEntity(requestDto);
        user.encodePassword(passwordEncoder.encode(user.getPassword()));
        user.addRole(UserRole.BUYER);

        userRepository.save(user);

        return userMapper.toResponse(user);
    }

}
