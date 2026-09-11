package com.giglister.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * Sends account emails. Built manually rather than relying on Spring Boot's
 * mail auto-configuration, so an unset SMTP host (local dev has none by
 * default) just logs the link instead of trying - and failing - to connect.
 * See application.yml.
 */
@Service
@Slf4j
public class MailService {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final String frontendBaseUrl;

    public MailService(
            @Value("${giglister.mail.host:}") String host,
            @Value("${giglister.mail.port:587}") int port,
            @Value("${giglister.mail.username:}") String username,
            @Value("${giglister.mail.password:}") String password,
            @Value("${giglister.mail.from}") String from,
            @Value("${giglister.frontend.base-url}") String frontendBaseUrl
    ) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendVerificationEmail(String to, String username, String token) {
        String link = frontendBaseUrl + "/email-bestaetigen?token=" + token;
        String subject = "Bitte bestätige deine E-Mail-Adresse bei GigLister";
        String body = "Hallo " + username + ",\n\n"
                + "bitte bestätige deine E-Mail-Adresse, um dein GigLister-Konto zu aktivieren:\n"
                + link + "\n\n"
                + "Der Link ist 24 Stunden gültig.\n\n"
                + "Falls du kein Konto erstellt hast, kannst du diese Mail ignorieren.";
        send(to, subject, body, link);
    }

    public void sendPasswordResetEmail(String to, String username, String token) {
        String link = frontendBaseUrl + "/passwort-zuruecksetzen?token=" + token;
        String subject = "Passwort zurücksetzen bei GigLister";
        String body = "Hallo " + username + ",\n\n"
                + "du (oder jemand in deinem Namen) hat ein neues Passwort angefordert. Klicke auf den Link, um ein neues Passwort zu vergeben:\n"
                + link + "\n\n"
                + "Der Link ist 1 Stunde gültig.\n\n"
                + "Falls du das nicht warst, kannst du diese Mail ignorieren - dein Passwort bleibt unverändert.";
        send(to, subject, body, link);
    }

    private void send(String to, String subject, String body, String logLink) {
        if (host == null || host.isBlank()) {
            log.info("[DEV] No SMTP configured - would send \"{}\" to {}: {}", subject, to, logLink);
            return;
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        // Only request AUTH/STARTTLS when there's actually a username to authenticate
        // with - a real SMTP provider needs both, but a local catch-all like Mailpit
        // (the docker-compose default) doesn't require auth at all, and asking for it
        // anyway makes JavaMail fail the connection since the server never advertises
        // an AUTH mechanism to satisfy the request.
        if (username != null && !username.isBlank()) {
            sender.setUsername(username);
            sender.setPassword(password);
            sender.getJavaMailProperties().put("mail.smtp.auth", "true");
            sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
