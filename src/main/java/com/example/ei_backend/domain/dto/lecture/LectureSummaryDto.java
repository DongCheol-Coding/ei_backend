package com.example.ei_backend.domain.dto.lecture;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureSummaryDto {

    private Long id;
    private String title;
    private int orderIndex;
    private int durationSec;
    private double progress;

}