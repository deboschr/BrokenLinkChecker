package com.unpar.skripsi.crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetcher untuk halaman same-host menggunakan Jsoup.
 * - ignoreHttpErrors=true agar tetap mendapatkan statusCode pada 4xx/5xx
 * - parse HTML hanya jika status < 400 dan Content-Type mengandung "text/html"
 */
public class JsoupFetcher {

    public static class Page {
        public final int statusCode;
        public final Document document;
        public final Instant fetchedAt;
        public final java.net.URI effectiveUri; // NEW

        public Page(int statusCode, Document document, Instant fetchedAt, java.net.URI effectiveUri) {
            this.statusCode = statusCode;
            this.document = document;
            this.fetchedAt = fetchedAt;
            this.effectiveUri = effectiveUri;
        }
    }

    public Page fetch(URI uri, String userAgent, int timeoutMillis) {
        try {
            Connection.Response resp = Jsoup.connect(uri.toString())
                    .userAgent(userAgent != null ? userAgent : "BrokenLinkChecker/1.0")
                    .timeout(timeoutMillis)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .execute();

            int code = resp.statusCode();
            Document doc = null;
            String ctype = resp.contentType();
            boolean isHtml = ctype != null && ctype.toLowerCase().contains("text/html");
            if (code >= 200 && code < 400 && isHtml) {
                doc = resp.parse();
            }

            java.net.URI effective = null;
            try { effective = resp.url().toURI(); } catch (Exception ignore) {}

            return new Page(code, doc, Instant.now(), effective);
        } catch (Exception e) {
            return new Page(0, null, Instant.now(), null);
        }
    }


    /** Ekstrak semua kandidat tautan dari elemen umum (href & src). */
    public List<String> extractLinks(Document doc) {
        List<String> out = new ArrayList<>();
        if (doc == null) return out;

        // <a href>
        for (Element a : doc.select("a[href]")) out.add(a.attr("href"));
        // <link href> (css/import)
        for (Element l : doc.select("link[href]")) out.add(l.attr("href"));
        // <img src>, <script src>, <source src>, dll.
        for (Element s : doc.select("[src]")) out.add(s.attr("src"));
        return out;
    }

    /** (Opsional) Ekstrak anchor dengan teks—pakai nanti jika butuh anchorText untuk BrokenLink. */
    public List<Anchor> extractAnchors(Document doc) {
        List<Anchor> out = new ArrayList<>();
        if (doc == null) return out;
        for (Element a : doc.select("a[href]")) {
            out.add(new Anchor(a.attr("href"), a.text()));
        }
        return out;
    }

    public record Anchor(String href, String text) {}
}
