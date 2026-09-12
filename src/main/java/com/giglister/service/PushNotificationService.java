package com.giglister.service;

import com.giglister.domain.DeviceToken;
import com.giglister.repository.DeviceTokenRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Push notifications for the Android app - currently just "a concert was added near your
 * saved home location" (see EventService.notifyNearbyUsers). Off by default (no
 * FirebaseApp configured) so local dev and the test suite never need real Firebase
 * credentials, same pattern as MailService's unset SMTP host: every method here degrades
 * to a harmless no-op rather than throwing, since a failed push should never be allowed
 * to break the event/registration action that triggered it.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${giglister.firebase.credentials-file:}")
    private String credentialsFile;

    private boolean enabled;

    @PostConstruct
    void init() {
        if (credentialsFile == null || credentialsFile.isBlank()) {
            log.info("[DEV] No Firebase credentials configured - push notifications are disabled");
            return;
        }
        try (FileInputStream in = new FileInputStream(credentialsFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            enabled = true;
        } catch (IOException e) {
            log.warn("Failed to initialize Firebase from '{}' - push notifications stay disabled: {}", credentialsFile, e.toString());
        }
    }

    @Transactional
    public void registerToken(Long userId, String token) {
        deviceTokenRepository.findByToken(token)
                .ifPresentOrElse(
                        existing -> existing.setUserId(userId),
                        () -> deviceTokenRepository.save(DeviceToken.builder().userId(userId).token(token).build())
                );
    }

    @Transactional
    public void unregisterToken(String token) {
        deviceTokenRepository.deleteByToken(token);
    }

    /** Best-effort - every device registered to this user gets the notification; a
     * device whose token FCM reports as no longer valid is removed so it isn't retried
     * forever. Silently does nothing when push isn't configured (see init()). */
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        if (!enabled) {
            return;
        }
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        for (DeviceToken deviceToken : tokens) {
            Message message = Message.builder()
                    .setToken(deviceToken.getToken())
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    .build();
            try {
                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    deviceTokenRepository.deleteByToken(deviceToken.getToken());
                } else {
                    log.warn("Push to user {} failed: {}", userId, e.toString());
                }
            }
        }
    }
}
