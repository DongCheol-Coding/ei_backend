package com.example.ei_backend.domain.dto.lecture;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresignResponse {

    private String uploadUrl;
    private String storageKey;

}