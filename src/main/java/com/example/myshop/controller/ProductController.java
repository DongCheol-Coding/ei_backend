package com.example.myshop.controller;

import com.example.myshop.domain.dto.ProductDto;
import com.example.myshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDto.Response> createProduct(
            @ModelAttribute ProductDto.Request request
    ) throws IOException {
        ProductDto.Response response = productService.createProduct(request);
        return ResponseEntity.ok(response);
    }
}
