package com.example.ei_backend.service;

import com.example.ei_backend.aws.S3Uploader;
import com.example.ei_backend.domain.dto.CourseDto;
import com.example.ei_backend.domain.dto.CourseProgressDto;
import com.example.ei_backend.domain.dto.CoursePurchasePreviewDto;
import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.domain.entity.UserCourse;
import com.example.ei_backend.exception.NotFoundException;
import com.example.ei_backend.repository.CourseRepository;
import com.example.ei_backend.repository.LectureRepository;
import com.example.ei_backend.repository.UserCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final S3Uploader s3Uploader;
    private final LectureRepository lectureRepository;
    private final CourseProgressService courseProgressService;

    @Value("${app.progress.complete-threshold:90.0}")
    private double completeThreshold;

    @Transactional
    public CourseDto.Response createProduct(CourseDto.CreateRequest request) throws IOException {
        // 1. 이미지 업로드
        String imageUrl = s3Uploader.upload(request.getImage(), "product-images");

        // 2. 엔티티 생성
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(imageUrl)
                .build();

        courseRepository.save(course);

        // 3. 응답 DTO 반환
        return CourseDto.Response.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .imageUrl(course.getImageUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public Course findById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("course"));
    }

    @Transactional
    public List<CourseDto.Summary> listAllPublicCourses() {
        List<Course> list = courseRepository
                .findByPublishedTrueAndDeletedFalse(Sort.by(Sort.Direction.DESC, "id")); // createdAt 있으면 그걸로
        return list.stream().map(this::toSummary).toList();
    }

//    /** 1) 전체 공개 코스 조회 (검색/페이징) */
//    @Transactional(readOnly = true)
//    public CourseDto.Page<CourseDto.Summary> findPublicCourses(
//            int page, int size, String q, String sort
//    ) {
//        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
//        Page<Course> result = (q == null || q.isBlank())
//                ? courseRepository.findByPublishedTrueAndDeletedFalse(pageable)
//                : courseRepository.findByPublishedTrueAndDeletedFalseAndTitleContainingIgnoreCase(q, pageable);
//
//        List<CourseDto.Summary> content = result.getContent().stream()
//                .map(this::toSummary)
//                .toList();
//
//        return CourseDto.Page.<CourseDto.Summary>builder()
//                .content(content)
//                .page(result.getNumber())
//                .size(result.getSize())
//                .totalElements(result.getTotalElements())
//                .totalPages(result.getTotalPages())
//                .last(result.isLast())
//                .build();
//    }

    /** 2) 공개/비공개 토글 */
    @Transactional
    public CourseDto.Summary setPublished(Long courseId, boolean published) {
        Course course = findById(courseId);
        course.setPublished(published);
        // JPA 더티체킹으로 flush 되지만, 명시 저장을 선호하면 아래 추가
        // courseRepository.save(course);
        return toSummary(course);
    }

    /** 내 코스 목록 (수강 중) - 진행률: CourseProgressService 기반 */
    @Transactional(readOnly = true)
    public CourseDto.Page<CourseDto.MyCourseItem> findMyCourses(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<UserCourse> result = userCourseRepository.findByUser_IdWithCourse(userId, pageable);

        List<CourseDto.MyCourseItem> items = result.getContent().stream()
                .map(uc -> {
                    var c = uc.getCourse();
                    Long courseId = c.getId();

                    // ✅ 새 진행률 로직 (코스 전체 기준)
                    double percent = courseProgressService.getCourseProgressPercent(userId, courseId);
                    var cnt = courseProgressService.getProgressCount(userId, courseId);

                    return CourseDto.MyCourseItem.builder()
                            .courseId(courseId)
                            .courseTitle(c.getTitle())
                            .imageUrl(c.getImageUrl())
                            .progress(CourseProgressDto.of(
                                    percent,
                                    (int) cnt.completedLectures(),
                                    (int) cnt.totalLectures(),
                                    completeThreshold
                            ))
                            .build();
                })
                .toList();

        return CourseDto.Page.<CourseDto.MyCourseItem>builder()
                .content(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }
    /** 결제 직전 노출용(공개 코스만) */
    @Transactional(readOnly = true)
    public CoursePurchasePreviewDto getPurchasePreview(Long courseId) {
        Course c = courseRepository.findByIdAndPublishedTrueAndDeletedFalse(courseId)
                .orElseThrow(() -> new NotFoundException("course"));

        int lectureCount = (int) lectureRepository.countByCourseId(courseId);
        int totalDuration = lectureRepository.sumDurationByCourseId(courseId);

        return CoursePurchasePreviewDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .price(c.getPrice())
                .imageUrl(c.getImageUrl())
                .lectureCount(lectureCount)
                .totalDurationSec(totalDuration)
                .build();
    }

    // ----- helpers -----
    private CourseDto.Summary toSummary(Course c) {
        return CourseDto.Summary.builder()
                .id(c.getId())
                .title(c.getTitle())
                .imageUrl(c.getImageUrl())
                .price(c.getPrice())
                .published(c.isPublished())
                .build();
    }

    private Sort parseSort(String sort) {
        // "createdAt,desc" 형태 → Sort 변환. 기본값은 최신순.
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "id");
        String[] parts = sort.split(",");
        String prop = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1])) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, prop);
    }

}
