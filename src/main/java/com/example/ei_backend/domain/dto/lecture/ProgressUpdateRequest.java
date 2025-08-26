package com.example.ei_backend.domain.dto.lecture;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProgressUpdateRequest {

    // 직렬화 키는 watchedSec, 입력은 watchedSec / watched_sec / positionSec 허용
    @JsonProperty("watchedSec")
    @JsonAlias({"watched_sec", "positionSec"})
    @NotNull(message = "watchedSec는 필수입니다.")
    @Min(value = 0, message = "watchedSec는 0 이상이어야 합니다.")
    private Integer watchedSec;

    private boolean completed;
}
