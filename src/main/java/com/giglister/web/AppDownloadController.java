package com.giglister.web;

import com.giglister.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The Android app's APK, for sideloading straight from the site (there's no Play Store
 * listing) - see UserMenu on the frontend for the "App herunterladen" entry point. Public
 * like /uploads/** (see SecurityConfig): the frontend only shows the link to a logged-in
 * visitor, but the APK itself isn't sensitive data worth gating behind a token, and a
 * direct link is exactly how an installer for a side-loaded app is normally shared anyway.
 */
@RestController
@Slf4j
public class AppDownloadController {

    private final Path downloadPath;

    public AppDownloadController(@Value("${giglister.app.download-path}") String downloadPath) {
        this.downloadPath = Path.of(downloadPath);
    }

    @GetMapping("/api/app/download")
    public ResponseEntity<byte[]> download() {
        byte[] apk;
        try {
            apk = Files.readAllBytes(downloadPath);
        } catch (IOException e) {
            log.warn("Android APK not found at {} - has it been built and placed there?", downloadPath, e);
            throw new NotFoundException("Die App steht aktuell nicht zum Download bereit.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"giglister.apk\"")
                .body(apk);
    }
}
