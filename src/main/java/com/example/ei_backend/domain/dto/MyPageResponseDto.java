package com.example.ei_backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPageResponseDto {

    private UserDto.Response user;
    private List<PaymentDto> payments = new ArrayList<>();
    private List<CourseProgressDto> coursesProgress = new ArrayList<>();

}
