package com.example.ei_backend.domain.dto;

import com.example.ei_backend.domain.UserRole;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

public class UserDto {

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @Email
        private String email;



        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$",
                message = "비밀번호는 8자 이상, 영문자+숫자+특수문자를 포함해야 합니다."
        )
        @ValidPassword
        private String password;

        private String name;

        private LocalDate birthDate;

        @Pattern(regexp = "^\\d{10,15}$")
        private String phone;

        private Set<UserRole> roles; // Optional: SELLER 선택 가능

        private String code;
    }


    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String email;
        private String name;
        private String token;
        private Set<UserRole> roles;

        private String message;


        public static Response fromEntity(User user, String token) {
            return Response.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .token(token)
                    .roles(user.getRoles())
                    .build();
        }

        public static Response message(String message) {
            Response response = new Response();
            response.message = message;
            return response;
        }
    }

    @Getter @Setter
    public static class LoginRequest {
        private String email;
        private String password;
    }

}
