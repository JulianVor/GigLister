package com.giglister.service;

import com.giglister.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Downloads an externally-hosted image (an admin approving a Submission is
 * the only caller) and stores it the same way a direct upload would.
 *
 * A server that fetches "whatever URL it's told to" is a classic SSRF
 * target - a request could otherwise be aimed at the server's own internal
 * network (e.g. http://backend:8080/..., or a cloud metadata endpoint).
 * Refusing to resolve to a private/loopback/link-local address, not
 * following redirects, and capping both content-type and size closes that
 * off without needing an allowlist of image hosts.
 */
@Service
@RequiredArgsConstructor
public class ImageFetchService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final int TIMEOUT_MS = 8000;

    private final UploadService uploadService;

    public String fetchAndStore(String url) {
        URI uri = parseHttpUri(url);
        assertNotInternalAddress(uri);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "GigLister-ImageFetch/1.0");
            connection.connect();

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new BadRequestException("Bild-URL konnte nicht geladen werden (HTTP " + status + ")");
            }
            String contentType = connection.getContentType();
            String normalizedContentType = contentType == null ? null : contentType.split(";")[0].trim();
            if (normalizedContentType == null || !ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
                throw new BadRequestException("Die URL liefert kein unterstütztes Bildformat (JPEG/PNG/WebP/GIF)");
            }

            byte[] data = readLimited(connection.getInputStream());
            return uploadService.storeBytes(data, normalizedContentType);
        } catch (IOException e) {
            throw new BadRequestException("Bild-URL konnte nicht geladen werden: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private URI parseHttpUri(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new BadRequestException("Ungültige Bild-URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new BadRequestException("Nur http/https-URLs sind erlaubt");
        }
        if (uri.getHost() == null) {
            throw new BadRequestException("Ungültige Bild-URL");
        }
        return uri;
    }

    private void assertNotInternalAddress(URI uri) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException e) {
            throw new BadRequestException("Host der Bild-URL konnte nicht aufgelöst werden");
        }
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isAnyLocalAddress() || address.isMulticastAddress()) {
                throw new BadRequestException("Diese Bild-URL zeigt auf eine interne Adresse und wird abgelehnt");
            }
        }
    }

    private byte[] readLimited(InputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > uploadService.maxFileSizeBytes()) {
                throw new BadRequestException("Die Datei ist zu groß (maximal " + uploadService.maxSizeMb() + " MB)");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
