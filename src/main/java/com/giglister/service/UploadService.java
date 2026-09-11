package com.giglister.service;

import com.giglister.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploaded images on local disk and hands back a browser-reachable
 * URL, built from the same public base URL the frontend uses to reach the
 * API directly (see .env.example) - so the URL keeps working when stored
 * back onto a Band/Location/Event's logoUrl/titleImageUrl field, no schema
 * change needed there.
 */
@Service
public class UploadService {

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final Path uploadDir;
    private final String publicBaseUrl;

    public UploadService(
            @Value("${giglister.upload.dir}") String uploadDir,
            @Value("${giglister.public.base-url}") String publicBaseUrl
    ) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory " + this.uploadDir, e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Keine Datei hochgeladen");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Die Datei ist zu groß (maximal 5 MB)");
        }
        String extension = ALLOWED_CONTENT_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Nur JPEG-, PNG-, WebP- oder GIF-Bilder sind erlaubt");
        }

        // A random filename, ignoring whatever name the client sent - sidesteps
        // path traversal and filename collisions in one move.
        String filename = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store uploaded file", e);
        }
        return publicBaseUrl + "/uploads/" + filename;
    }
}
