package com.giglister.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GET /api/app/download (see AppDownloadController) - public like /uploads/**, no auth
 * needed at the backend, since the frontend link is what's gated to logged-in visitors
 * (see UserMenu), not the APK bytes themselves. Runs with the repo root as CWD (same as
 * `mvn spring-boot:run` does), so the real android/release/app.apk is exactly where
 * giglister.app.download-path's default expects it, without any test-only override.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppDownloadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void appApkDownloadsWithoutAuthAndTheRightContentType() throws Exception {
        mockMvc.perform(get("/api/app/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.android.package-archive"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("giglister.apk")));
    }
}
