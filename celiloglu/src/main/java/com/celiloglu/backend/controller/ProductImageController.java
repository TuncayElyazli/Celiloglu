package com.celiloglu.backend.controller;

import com.celiloglu.backend.service.ProductImageStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.NoSuchFileException;
import java.util.concurrent.TimeUnit;

@Controller
public class ProductImageController {

    private final ProductImageStorageService imageStorageService;

    public ProductImageController(ProductImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/product-images/{filename:.+}")
    public ResponseEntity<Resource> image(@PathVariable("filename") String filename) {
        try {
            Resource image = imageStorageService.load(filename);
            MediaType type = filename.endsWith(".png")
                    ? MediaType.IMAGE_PNG
                    : MediaType.IMAGE_JPEG;
            return ResponseEntity.ok()
                    .contentType(type)
                    .header("X-Content-Type-Options", "nosniff")
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                    .body(image);
        } catch (NoSuchFileException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
