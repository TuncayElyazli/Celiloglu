package com.celiloglu.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

public interface ProductImageStorageService {

    String URL_PREFIX = "/product-images/";
    long MAX_FILE_BYTES = 1024L * 1024L;

    String store(MultipartFile file) throws IOException;

    Resource load(String filename) throws NoSuchFileException;

    void deleteNewUpload(String imageUrl);
}
