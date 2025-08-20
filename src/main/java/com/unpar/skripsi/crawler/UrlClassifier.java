package com.unpar.skripsi.crawler;

import java.net.URI;

/**
 * Classifier for routing:
 * - same-host "webpages" (to be fetched with Jsoup)
 * - other links (to be checked with HttpClient HEAD/GET)
 *
 * Policy:
 * - Same host = exact host match (case-insensitive).
 * - Consider as "webpage" if:
 *   - path ends with "/" (directory), or
 *   - path has no extension (no '.'), or
 *   - path ends with common HTML-ish extensions (.html/.htm/.php/.asp/.aspx/.jsp/.jspx)
 *   Non-HTML (images, pdf, zip, etc.) are treated as non-webpage links.
 */
public class UrlClassifier {

    public boolean isSameHost(URI a, URI b) {
        if (a == null || b == null) return false;
        String ha = a.getHost(), hb = b.getHost();
        return ha != null && hb != null && ha.equalsIgnoreCase(hb);
    }

    /** Return true if candidate should be fetched via Jsoup as "same-host webpage". */
    public boolean isSameHostWebpage(URI seed, URI candidate) {
        if (!isSameHost(seed, candidate)) return false;

        String path = candidate.getPath();
        if (path == null || path.isEmpty() || path.endsWith("/")) return true;

        String lower = path.toLowerCase();

        // Likely HTML pages
        if (lower.endsWith(".html") || lower.endsWith(".htm")
                || lower.endsWith(".php")
                || lower.endsWith(".asp") || lower.endsWith(".aspx")
                || lower.endsWith(".jsp") || lower.endsWith(".jspx")) {
            return true;
        }

        // If the path contains no dot, treat as a "pretty URL" page: /about, /contact, /products/latest
        return !lower.contains(".");
    }
}
