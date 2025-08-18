package com.example.ei_backend.service;

import com.example.ei_backend.domain.dto.lecture.LectureDetailDto;
import com.example.ei_backend.domain.dto.lecture.LectureSummaryDto;
import com.example.ei_backend.domain.entity.VideoAsset;
import com.example.ei_backend.exception.NotFoundException;
import com.example.ei_backend.mapper.LectureMapper;
import com.example.ei_backend.repository.CourseRepository;
import com.example.ei_backend.repository.LectureProgressRepository;
import com.example.ei_backend.repository.LectureRepository;
import com.example.ei_backend.repository.UserCourseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.security.SecurityUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureQueryService {

    private final LectureRepository lectureRepository;
    private final LectureProgressRepository progressRepository;
    private final UserCourseRepository enrollRepository;
    private final LectureMapper lectureMapper;

    @PreAuthorize("hasRole('ADMIN') or @enrollPerm.canAccessCourse(#userId, #courseId)")
    public List<LectureSummaryDto> listForUser(Long userId, Long courseId) {
        var lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        return lectures.stream().map(l -> {
            var lp = progressRepository.findByUserIdAndLectureId(userId, l.getId()).orElse(null);
            double prog = (l.getDurationSec() == 0 || lp == null) ? 0.0 :
                    Math.min(1.0, (double) lp.getWatchedSec() / l.getDurationSec());
            return lectureMapper.toSummary(l, prog);
        }).toList();
    }

    @PreAuthorize("hasRole('ADMIN') or @enrollPrem.canAccessLecture(#userId, #lecturedId)")
    public LectureDetailDto getForUser(Long userId, Long lectureId) {
        var l = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("lecture"));
        var lp = progressRepository.findByUserIdAndLectureId(userId, lectureId).orElse(null);
        double prog = (l.getDurationSec() == 0 || lp == null) ? 0.0 :
                Math.min(1.0, (double) lp.getWatchedSec() / l.getDurationSec());
        String videoUrl = (l.getVideo() != null && l.getVideo().getStatus() == VideoAsset.Status.READY)
                ? l.getVideo().getUrl() : null;
        return lectureMapper.toDetail(l, videoUrl, prog);
    }


}
