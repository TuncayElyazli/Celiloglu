package com.celiloglu.backend.service;

import com.celiloglu.backend.service.impl.ProductImageStorageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductImageStorageServiceImplTest {

    @Test
    @DisplayName("Şəkil faylı mövcud qovluqdan düzgün yüklənir")
    void testLoadExistingFile(@TempDir Path tempDir) throws IOException {
        String filename = UUID.randomUUID() + ".jpg";
        Path imageFile = tempDir.resolve(filename);
        Files.write(imageFile, new byte[]{1, 2, 3});

        ProductImageStorageServiceImpl storageService = new ProductImageStorageServiceImpl(tempDir.toString());
        Resource resource = storageService.load(filename);

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertEquals(3, resource.contentLength());
    }

    @Test
    @DisplayName("Şəkil adı UUID formatında olmadıqda NoSuchFileException atılır")
    void testLoadInvalidFilename(@TempDir Path tempDir) {
        ProductImageStorageServiceImpl storageService = new ProductImageStorageServiceImpl(tempDir.toString());
        assertThrows(NoSuchFileException.class, () -> storageService.load("malicious..path.jpg"));
        assertThrows(NoSuchFileException.class, () -> storageService.load("random.txt"));
    }

    @Test
    @DisplayName("Şəkil mövcud olmadıqda NoSuchFileException atılır")
    void testLoadNonExistentFile(@TempDir Path tempDir) {
        ProductImageStorageServiceImpl storageService = new ProductImageStorageServiceImpl(tempDir.toString());
        String nonExistent = UUID.randomUUID() + ".jpg";
        assertThrows(NoSuchFileException.class, () -> storageService.load(nonExistent));
    }
}
