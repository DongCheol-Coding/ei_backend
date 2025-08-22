package com.example.ei_backend.service;

import com.example.ei_backend.domain.dto.lecture.*;
import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.domain.entity.Lecture;
import com.example.ei_backend.domain.entity.VideoAsset;
import com.example.ei_backend.exception.NotFoundException;
import com.example.ei_backend.mapper.LectureMapper;
import com.example.ei_backend.repository.CourseRepository;
import com.example.ei_backend.repository.LectureRepository;
import com.example.ei_backend.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LectureCommandService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final LectureMapper lectureMapper;
    private final VideoAssetRepository videoAssetRepository;

    @Value("${app.cdn-base-url:}")          // 선택: 있으면 CDN URL 사용
    private String cdnBaseUrl;

    @Value("${cloud.aws.s3.bucket}")        // 없으면 S3 퍼블릭 URL 조립용
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Transactional
    public LectureDto create(Long courseId, LectureCreateRequest req) {
        Course c = courseRepository.findById(courseId).orElseThrow(() -> new NotFoundException("course"));
        Lecture l = Lecture.create(
                c,
                req.getTitle(),
                req.getDescription(),
                req.getOrderIndex(),
                req.getIsPublic()
        );

        lectureRepository.save(l);
        return lectureMapper.toDto(l);
    }

    @Transactional
    public LectureDto update(Long lectureId, LectureUpdateRequest request) {
        Lecture l = lectureRepository.findById(lectureId).orElseThrow(() -> new NotFoundException("lecture"));
        l.update(request.getTitle(), request.getDescription(), request.getOrderIndex(), request.getIsPublic());
        return lectureMapper.toDto(l);
    }

    @Transactional
    public void delete(Long lectureId) {
        Lecture l = lectureRepository.findById(lectureId).orElseThrow(() -> new NotFoundException("lecture"));
        // S3 삭제는 나중에
        lectureRepository.delete(l);
    }

    // presign/confirm을 쓰고 싶다면 여기 유지(지금은 최소 구현이므로 uploadUrl 빈 값)
    @Transactional
    public PresignResponse presign(Long lectureId, PresignRequest request) {
        Lecture l = lectureRepository.findById(lectureId).orElseThrow(() -> new NotFoundException("lecture"));
        String key = buildStorageKey(l, request.getFileName());
        String uploadUrl = ""; // 나중에 presigned URL 붙이면 됨
        return new PresignResponse(uploadUrl, key);
    }

    @Transactional
    public LectureDto confirm(Long lectureId, ConfirmVideoRequest req){
        Lecture l = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("lecture"));

        VideoAsset asset = videoAssetRepository.findByLectureId(lectureId)
                .orElseGet(() -> {
                    VideoAsset v = VideoAsset.of(req.getStorageKey());
                    l.attachVideo(v);
                    return videoAssetRepository.save(v);
                });

        String url = resolvePublicUrl(req.getStorageKey());
        asset.markReady(url, req.getDurationSec(), req.getSizeBytes());

        // ✅ 반드시 동기화: 비디오에 설정된 길이를 Lecture에도 반영
        if (asset.getDurationSec() > 0) {
            // 편한 방법 1) 세터/메서드 추가
            l.updateDurationFromVideo(asset.getDurationSec());
            // 또는 편한 방법 2) 직접 필드 반영용 메서드 만들어 사용
            // l.setDurationSec(asset.getDurationSec());
        }

        return lectureMapper.toDto(l);
    }



    private String buildStorageKey(Lecture lecture, String fileName) {
        Long courseId = lecture.getCourse().getId();
        Long lectureId = lecture.getId();
        String safe = (fileName == null ? "video.mp4" : fileName.replaceAll("[^A-Za-z0-9._-]", "_"));
        String uuid = java.util.UUID.randomUUID().toString();
        return String.format("courses/%d/lectures/%d/%s-%s", courseId, lectureId, uuid, safe);
    }

    private String resolvePublicUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return null;

        // CDN 우선
        if (cdnBaseUrl != null && !cdnBaseUrl.isBlank()) {
            return cdnBaseUrl.endsWith("/") ? cdnBaseUrl + storageKey
                    : cdnBaseUrl + "/" + storageKey;
        }

        // CDN 설정이 없으면 S3 퍼블릭 URL 사용
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + storageKey;
    }



}
