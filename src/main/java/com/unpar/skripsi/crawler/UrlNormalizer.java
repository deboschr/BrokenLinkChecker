package com.unpar.skripsi.crawler;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * URL normalizer:
 * - Resolve relative against base
 * - Only allow http/https
 * - Lowercase host
 * - Drop fragments (#...)
 * - Remove default ports (80/443)
 * - Ensure non-empty path ("/")
 * - Normalize path (remove ./ and ../ where possible)
 */
public class UrlNormalizer {

    /** Normalize a candidate href/src against a base page URI. Returns null if invalid/unsupported. */
    public URI normalize(URI base, String candidate) {
        if (candidate == null || candidate.isBlank()) return null;

        // ignore javascript:, mailto:, tel:, data:, ftp:, file:
        String lowerTrim = candidate.trim().toLowerCase();
        if (lowerTrim.startsWith("javascript:")
                || lowerTrim.startsWith("mailto:")
                || lowerTrim.startsWith("tel:")
                || lowerTrim.startsWith("data:")
                || lowerTrim.startsWith("ftp:")
                || lowerTrim.startsWith("file:")) {
            return null;
        }

        URI resolved;
        try {
            URI cand = new URI(candidate.trim());
            resolved = (base == null) ? cand : base.resolve(cand);
        } catch (URISyntaxException e) {
            return null;
        }

        String scheme = resolved.getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
            return null;
        }

        String host = resolved.getHost();
        if (host == null || host.isBlank()) return null;

        // normalize parts
        host = host.toLowerCase();
        int port = resolved.getPort();
        String path = resolved.getPath();
        if (path == null || path.isEmpty()) path = "/";

        try {
            URI normalized = new URI(
                    scheme,
                    resolved.getUserInfo(),
                    host,
                    isDefaultPort(scheme, port) ? -1 : port,
                    path,
                    resolved.getQuery(),
                    null // drop fragment
            ).normalize();

            // Ensure path not empty after normalize
            if (normalized.getPath() == null || normalized.getPath().isEmpty()) {
                normalized = new URI(
                        normalized.getScheme(),
                        normalized.getUserInfo(),
                        normalized.getHost(),
                        normalized.getPort(),
                        "/",
                        normalized.getQuery(),
                        null
                );
            }

            return normalized;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /** Normalize an absolute URL string. Returns null if invalid/unsupported. */
    public URI normalizeAbsolute(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return normalize(null, url);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return port == -1 || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
    }
}
