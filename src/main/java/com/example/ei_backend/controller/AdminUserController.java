package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.UserDto;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.mapper.UserMapper;
import com.example.ei_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDto.Response>>> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phoneSuffix,
            @RequestParam(required = false) String role
    ) {
        List<User> users = authService.searchUser(name, phoneSuffix, role);
        List<UserDto.Response> body = users.stream()
                .map(userMapper::toResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}
