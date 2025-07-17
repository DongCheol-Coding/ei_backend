package com.example.myshop.service;

import com.example.myshop.domain.dto.UserDto;
import com.example.myshop.domain.entity.User;
import com.example.myshop.mapper.UserMapper;
import com.example.myshop.repository.UserRepository;
import com.example.myshop.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public UserDto.Response signup(UserDto.Request requestDto) {
        if (userRepository.existsByEmailAndIsDeletedFalse(requestDto.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        User user = userMapper.toEntity(requestDto);
        user.encodePassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        return userMapper.toResponse(user);
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

}
