package com.example.myshop.controller;

import com.example.myshop.domain.dto.ChangePasswordRequest;
import com.example.myshop.domain.dto.DeleteAccountRequest;
import com.example.myshop.domain.dto.UserDto;
import com.example.myshop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto.Response> signup(@RequestBody @Valid UserDto.Request requestDto) {
        UserDto.Response responseDto = authService.signup(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getUserId(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(request.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto.Response> login(@RequestBody UserDto.LoginRequest request) {
        UserDto.Response response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }


}
