package com.example.ei_backend.domain.dto.lecture;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdateRequest {


    @NotNull
    private Integer watchedSec;

    private boolean completed;

}