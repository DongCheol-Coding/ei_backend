package com.example.ei_backend.domain.dto.lecture;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LectureCreateRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Integer orderIndex;

    @NotNull
    private Boolean isPublic;

    private Integer durationSec;
    private Long sizeBytes;

}