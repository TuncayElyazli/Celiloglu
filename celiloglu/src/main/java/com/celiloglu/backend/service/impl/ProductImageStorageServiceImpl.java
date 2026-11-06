package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.service.ProductImageStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProductImageStorageServiceImpl implements ProductImageStorageService {

    private static final long MAX_PIXELS = 16_000_000L;
    private static final Pattern FILE_NAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)");
    private static final Logger log = LoggerFactory.getLogger(ProductImageStorageServiceImpl.class);

    private final Path directory;
    private final Path fallbackDirectory;

    public ProductImageStorageServiceImpl(
            @Value("${app.product-image-directory:uploads/products}") String directory) {
        Path inputPath = Path.of(directory);
        Path direct = inputPath.toAbsolutePath().normalize();
        Path parent = !inputPath.isAbsolute()
                ? Path.of("..", directory).toAbsolutePath().normalize()
                : null;

        if (Files.exists(direct)) {
            this.directory = direct;
            this.fallbackDirectory = (parent != null && Files.exists(parent) && !direct.equals(parent)) ? parent : null;
        } else if (parent != null && Files.exists(parent)) {
            this.directory = parent;
            this.fallbackDirectory = direct;
        } else {
            this.directory = direct;
            this.fallbackDirectory = (parent != null && !direct.equals(parent)) ? parent : null;
        }
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Şəkil faylı boşdur.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Şəkil maksimum 1 MB ola bilər.");
        }

        // Faylın adına və göndərilən MIME növünə etibar etmirik; şəklin özünü oxuyuruq.
        try (InputStream input = file.getInputStream();
                ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new IllegalArgumentException("Şəkil oxunmadı. PNG və ya JPG seçin.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Yalnız PNG və JPG şəkilləri qəbul olunur.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) {
                    throw new IllegalArgumentException("Yalnız PNG və JPG şəkilləri qəbul olunur.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new IllegalArgumentException("Şəklin ölçüsünü kiçildin: maksimum 16 meqapiksel.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("Şəkil oxunmadı. Başqa şəkil seçin.");
                }
                String extension = format.equals("png") ? "png" : "jpg";
                String filename = UUID.randomUUID() + "." + extension;
                Files.createDirectories(directory);
                Path destination = directory.resolve(filename);
                boolean created = false;
                try {
                    try (OutputStream output = Files.newOutputStream(destination,
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                        created = true;
                        if (!ImageIO.write(image, extension, output)) {
                            throw new IOException("Şəkil yazılmadı.");
                        }
                    }
                } catch (IOException | RuntimeException ex) {
                    try {
                        if (created) {
                            Files.deleteIfExists(destination);
                        }
                    } catch (IOException cleanupError) {
                        ex.addSuppressed(cleanupError);
                    }
                    throw ex;
                }
                return URL_PREFIX + filename;
            } finally {
                reader.dispose();
            }
        }
    }

    @Override
    public Resource load(String filename) throws NoSuchFileException {
        if (!FILE_NAME.matcher(filename).matches()) {
            throw new NoSuchFileException(filename);
        }
        Path path = directory.resolve(filename).normalize();
        if (isValidFile(path, directory)) {
            return new FileSystemResource(path);
        }
        if (fallbackDirectory != null) {
            Path fallbackPath = fallbackDirectory.resolve(filename).normalize();
            if (isValidFile(fallbackPath, fallbackDirectory)) {
                return new FileSystemResource(fallbackPath);
            }
        }
        throw new NoSuchFileException(filename);
    }

    private boolean isValidFile(Path path, Path baseDir) {
        return path.startsWith(baseDir) && !Files.isSymbolicLink(path)
                && Files.isRegularFile(path) && Files.isReadable(path);
    }

    // DB-yə yazılma uğursuz olarsa, yalnız bu sorğuda yüklənmiş yeni şəkli
    // təmizləyirik.
    @Override
    public void deleteNewUpload(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) {
            return;
        }
        String filename = imageUrl.substring(URL_PREFIX.length());
        if (!FILE_NAME.matcher(filename).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(directory.resolve(filename));
            if (fallbackDirectory != null) {
                Files.deleteIfExists(fallbackDirectory.resolve(filename));
            }
        } catch (IOException ex) {
            log.warn("Uğursuz sorğunun yeni şəkli təmizlənmədi: {}", filename, ex);
        }
    }
}
