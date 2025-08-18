package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoAssetRepository extends JpaRepository<VideoAsset, Long> {
    Optional<VideoAsset> findByLectureId(Long lectureId);
}
