package com.example.myshop.security;

import com.example.myshop.domain.dto.UserDto;
import com.example.myshop.domain.entity.User;
import com.example.myshop.mapper.UserMapper;
import com.example.myshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    public UserDto.Response signup(UserDto.Request requestDto) {
        if (userRepository.existEmail(requestDto.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        User user = userMapper.toEntity(requestDto);
        user.encodePassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        return userMapper.toResponse(user);
    }

}
