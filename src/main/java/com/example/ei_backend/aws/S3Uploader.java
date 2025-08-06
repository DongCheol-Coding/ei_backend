package com.example.ei_backend.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Uploader {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    private final S3Client s3Client;

    public String upload(MultipartFile file, String dirName) throws IOException {
        String fileName = dirName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(file.getBytes())
        );

        return getFileUrl(fileName);
    }

    public void delete(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl); // S3 객체 key 추출

        s3Client.deleteObject(builder -> builder
                .bucket(bucketName)
                .key(key)
                .build());

        log.info("✅ S3에서 파일 삭제 완료: {}", key);
    }

    private String extractKeyFromUrl(String fileUrl) {
        // ex: https://bucket-name.s3.ap-northeast-2.amazonaws.com/profile-images/abc123.jpg
        int index = fileUrl.indexOf("profile-images"); // 업로드 디렉토리 이름 기준
        if (index == -1) {
            throw new IllegalArgumentException("S3 이미지 URL에서 key를 추출할 수 없습니다: " + fileUrl);
        }
        return fileUrl.substring(index);
    }
    private String getFileUrl(String fileName) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;
    }


}

