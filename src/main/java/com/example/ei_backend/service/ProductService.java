package com.example.ei_backend.service;

import com.example.ei_backend.aws.S3Uploader;
import com.example.ei_backend.domain.dto.ProductDto;
import com.example.ei_backend.domain.entity.Product;
import com.example.ei_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public ProductDto.Response createProduct(ProductDto.Request request) throws IOException {
        // 1. 이미지 업로드
        String imageUrl = s3Uploader.upload(request.getImage(), "product-images");

        // 2. 엔티티 생성
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(imageUrl)
                .build();

        productRepository.save(product);

        // 3. 응답 DTO 반환
        return ProductDto.Response.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .build();
    }

}
