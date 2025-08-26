package com.example.ei_backend.service;

import com.example.ei_backend.domain.entity.LectureProgress;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.LectureProgressRepository;
import com.example.ei_backend.repository.LectureRepository;
import com.example.ei_backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LectureProgressService {

    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final UserRepository userRepository; // ← 주입

    @Value("${app.progress.complete-threshold-ratio:0.9}")
    private double completeThresholdRatio;

    @Transactional
    public void updateProgress(Long userId, Long lectureId, int positionSec, boolean clientCompleted) {
        var lecture = lectureRepository.getReferenceById(lectureId);
        var userRef = userRepository.getReferenceById(userId);

        var lp = lectureProgressRepository.findByUserIdAndLectureId(userId, lectureId)
                .orElseGet(() -> LectureProgress.start(userRef, lecture));

        lp.applyProgress(positionSec, lecture.getDurationSec(), completeThresholdRatio, clientCompleted);
        lectureProgressRepository.save(lp);
    }
}