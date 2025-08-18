package com.example.ei_backend.mapper;

import com.example.ei_backend.domain.dto.lecture.LectureDetailDto;
import com.example.ei_backend.domain.dto.lecture.LectureDto;
import com.example.ei_backend.domain.dto.lecture.LectureSummaryDto;
import com.example.ei_backend.domain.entity.Lecture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LectureMapper {
    @Mapping(target = "courseId", source = "course.id")
    LectureDto toDto(Lecture e);

    @Mapping(target = "id", source = "e.id")
    @Mapping(target = "title", source = "e.title")
    @Mapping(target = "orderIndex", source = "e.orderIndex")
    @Mapping(target = "durationSec", source = "e.durationSec")
    @Mapping(target = "progress", expression = "java(progress)")
    LectureSummaryDto toSummary(Lecture e, double progress);

    @Mapping(target = "id", source = "e.id")
    @Mapping(target = "courseId", source = "e.course.id")
    @Mapping(target = "title", source = "e.title")
    @Mapping(target = "description", source = "e.description")
    @Mapping(target = "durationSec", source = "e.durationSec")
    @Mapping(target = "videoUrl", expression = "java(videoUrl)")
    @Mapping(target = "progress", expression = "java(progress)")
    LectureDetailDto toDetail(Lecture e, String videoUrl, double progress);
}
