package com.example.myshop.domain.dto;

import com.example.myshop.domain.entity.User;
import lombok.*;

public class UserDto {

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {

    private String email;
    private String password;
    private String name;

    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String email;
        private String name;
        private String token;

        public static Response fromEntity(User user, String token) {
            return Response.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .token(token)
                    .build();
        }
    }

    @Getter @Setter
    public static class LoginRequest {
        private String email;
        private String password;
    }

}
