package com.giglister.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giglister.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Images are always uploaded, never entered as an external URL by hand - this
 * covers the endpoint that backs that: content-type/size validation, that the
 * stored file is served back out under /uploads/** without auth, and that the
 * upload itself requires a logged-in user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void uploadingAndServingAnImageRoundTrips() throws Exception {
        String token = register("uploader@example.com", "password123", "Uploader");

        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", pngBytes);

        var result = mockMvc.perform(multipart("/api/uploads").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        String url = objectMapper.readTree(result.getResponse().getContentAsString()).get("url").asText();
        org.assertj.core.api.Assertions.assertThat(url).matches("http://localhost:8080/uploads/[\\w-]+\\.png");

        String path = url.substring("http://localhost:8080".length());
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes));
    }

    @Test
    void uploadRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/uploads").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsDisallowedContentTypes() throws Exception {
        String token = register("uploader2@example.com", "password123", "Uploader2");
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/api/uploads").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFilesOverTheSizeLimit() throws Exception {
        String token = register("uploader3@example.com", "password123", "Uploader3");
        byte[] tooBig = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "huge.png", "image/png", tooBig);
        mockMvc.perform(multipart("/api/uploads").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private String register(String email, String password, String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password, "username", username))))
                .andExpect(status().isCreated());

        String verificationToken = userRepository.findByEmailIgnoreCase(email).orElseThrow().getVerificationToken();
        var result = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", verificationToken))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
