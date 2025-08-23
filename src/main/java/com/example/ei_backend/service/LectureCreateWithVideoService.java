package com.example.ei_backend.service;

import com.example.ei_backend.aws.S3Uploader;
// import com.example.ei_backend.aws.S3UrlKeyExtractor; // 유틸이 없으면 사용 안 함
import com.example.ei_backend.domain.dto.lecture.LectureCreateRequest;
import com.example.ei_backend.domain.dto.lecture.LectureDetailDto;
import com.example.ei_backend.domain.dto.lecture.LectureUpdateRequest;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class LectureCreateWithVideoService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final LectureMapper lectureMapper;
    private final S3Uploader s3Uploader;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Transactional
    public LectureDetailDto create(Long courseId, LectureCreateRequest request, MultipartFile video) throws IOException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("course"));

        Lecture lecture = Lecture.create(
                course, request.getTitle(), request.getDescription(),
                request.getOrderIndex(), request.getIsPublic()
        );
        lectureRepository.save(lecture);

        // 추가: 파일 없어도 durationSec이 오면 강의에 반영
        if (request.getDurationSec() != null) {
            lecture.updateDurationFromVideo(request.getDurationSec());
        }

        if (video != null && !video.isEmpty()) {
            String dir = String.format("courses/%d/lectures/%d", courseId, lecture.getId());
            String url = s3Uploader.upload(video, dir);

            String prefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
            String key = url.startsWith(prefix) ? url.substring(prefix.length()) : url;

            int duration = (request.getDurationSec() == null) ? 0 : request.getDurationSec();
            long size = (request.getSizeBytes() != null) ? request.getSizeBytes() : video.getSize();

            VideoAsset asset = videoAssetRepository.findByLectureId(lecture.getId()).orElse(null);
            if (asset == null) {
                asset = VideoAsset.of(key);
                asset.markReady(url, duration, size);
                lecture.attachVideo(asset);                 // ⬅︎ 새 자산: attach로 동기화
                videoAssetRepository.save(asset);
            } else {
                // 기존 자산 갱신
                // (key 변경 메서드가 있다면 갱신)
                asset.markReady(url, duration, size);
                if (duration > 0) {
                    lecture.updateDurationFromVideo(duration); // ⬅︎ ✅ 기존 자산: 동기화 보장
                }
            }
        }

        String videoUrl = (lecture.getVideo()!=null && lecture.getVideo().getStatus()==VideoAsset.Status.READY)
                ? lecture.getVideo().getUrl() : null;

        return lectureMapper.toDetail(lecture, videoUrl, 0.0);
    }


    @Transactional
    public LectureDetailDto update(Long lectureId,
                                   LectureUpdateRequest req,
                                   MultipartFile video) {

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("lecture"));

        // 1) 텍스트 먼저 업데이트
        lecture.update(req.getTitle(), req.getDescription(),
                req.getOrderIndex(), req.getIsPublic());

        VideoAsset asset = videoAssetRepository.findByLectureId(lectureId).orElse(null);

        // 2) 새 영상이 온 경우에만 업로드 & 비디오 자원 갱신
        if (video != null && !video.isEmpty()) {
            String dir = String.format("courses/%d/lectures/%d",
                    lecture.getCourse().getId(), lecture.getId());

            String url;
            try {
                url = s3Uploader.upload(video, dir);
            } catch (IOException e) {
                throw new RuntimeException("S3 upload failed", e);
            }

            String prefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
            String key = url.startsWith(prefix) ? url.substring(prefix.length()) : url;

            // duration: 요청값이 있으면 사용, 없으면 0 (또는 기존값)
            int duration = (req.getDurationSec() != null)
                    ? req.getDurationSec()
                    : (asset != null ? asset.getDurationSec() : 0);

            if (asset == null) {
                asset = VideoAsset.of(key);
                asset.markReady(url, duration, video.getSize());
                lecture.attachVideo(asset);
                videoAssetRepository.save(asset);
            } else {
                // 필요 시 storageKey 갱신 메서드가 있다면 호출
                asset.markReady(url, duration, video.getSize());
                lecture.updateDurationFromVideo(duration);
            }
        } else {
        // 영상 파일이 없고, duration만 텍스트로 들어왔으면 갱신
        if (req.getDurationSec() != null) {
            if (asset != null) {
                asset.setDurationSec(req.getDurationSec());
                videoAssetRepository.save(asset);
            }
            lecture.updateDurationFromVideo(req.getDurationSec()); // ⬅️ null 아닐 때만 호출
        }
    }

        String videoUrl = (lecture.getVideo() != null
                && lecture.getVideo().getStatus() == VideoAsset.Status.READY)
                ? lecture.getVideo().getUrl() : null;

        return lectureMapper.toDetail(lecture, videoUrl, 0.0);
    }

}
