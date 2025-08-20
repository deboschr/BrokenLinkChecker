package com.unpar.skripsi.crawler;

import com.unpar.skripsi.Service;
import com.unpar.skripsi.model.BrokenLink;
import com.unpar.skripsi.model.WebpageLink;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract crawler:
 * - Menyediakan util umum untuk BFS/DFS: normalisasi & kanonisasi URL, fetch & parse anchors,
 *   dedup link check, emit BrokenLink/WebpageLink, occurrence, dan limits.
 * - Subclass (BFS/DFS) hanya perlu mengimplementasikan strategi traversal & enqueue.
 */
public abstract class Crawler {

    protected final Service service;
    protected final UrlNormalizer normalizer = new UrlNormalizer();
    protected final UrlClassifier classifier = new UrlClassifier();
    protected final JsoupFetcher fetcher = new JsoupFetcher();
    protected final HttpHeadChecker headChecker;

    // Registries / repositories (state per-run)
    protected final PageRepository pageRepo = new PageRepository();           // halaman same-host + meta
    protected final LinkCatalog linkCatalog = new LinkCatalog();              // link unik yang sudah dicek
    protected final OccurrenceIndex occurrenceIndex = new OccurrenceIndex();  // target -> sumber

    // Runtime config
    protected final String userAgent;
    protected final int timeoutMillis;

    // Limits (0 = unlimited; disarankan set untuk DFS rekursif)
    protected final int maxDepth;
    protected final int maxPages;

    protected Crawler(Service service,
                      String userAgent,
                      int timeoutMillis,
                      boolean followRedirects,
                      int maxDepth,
                      int maxPages) {
        this.service = service;
        this.userAgent = userAgent;
        this.timeoutMillis = timeoutMillis;
        this.headChecker = new HttpHeadChecker(followRedirects);
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
    }

    /** Entry point — subclass mengimplementasikan traversal (BFS iteratif / DFS rekursif). */
    public abstract void crawl(String seedUrl);

    // ===================== Util umum =====================

    /** Fetch lalu pilih URL kanonik (efektif pasca-redirect, ternormalisasi). */
    protected CanonicalPage fetchCanonical(URI input) {
        JsoupFetcher.Page pr = fetcher.fetch(input, userAgent, timeoutMillis);
        URI canonical = input;
        if (pr.effectiveUri != null) {
            URI eff = normalizer.normalizeAbsolute(pr.effectiveUri.toString());
            if (eff != null) canonical = eff;
        }
        return new CanonicalPage(canonical, pr);
    }

    /**
     * Siapkan meta untuk halaman kanonik.
     * @return true bila perlu SKIP (mis. duplikat kanonik sudah ada / limit halaman tercapai)
     */
    protected boolean preparePageAndMaybeSkip(URI canonical, JsoupFetcher.Page pr, URI original) {
        // Jika hasil kanonik berbeda dari original & sudah pernah diproses → skip duplikat (http→https, trailing slash)
        if (!canonical.equals(original) && pageRepo.contains(canonical)) return true;

        if (maxPages > 0 && pageRepo.size() >= maxPages) return true;

        PageRepository.Meta meta = pageRepo.getOrCreate(canonical);
        meta.statusCode = pr.statusCode;
        meta.visitedAt = pr.fetchedAt;
        return false;
    }

    /** Bangun peta anchor (URI -> text) dari <a href> pada halaman (base = canonical). */
    protected Map<URI, String> buildAnchorMap(URI base, JsoupFetcher.Page pr) {
        Map<URI, String> map = new HashMap<>();
        if (pr.document == null) return map;
        for (JsoupFetcher.Anchor a : fetcher.extractAnchors(pr.document)) {
            URI u = normalizer.normalize(base, a.href());
            if (u == null) continue;
            String txt = a.text() == null ? "" : a.text().trim().replaceAll("\\s+", " ");
            if (!txt.isEmpty()) map.putIfAbsent(u, txt);
        }
        return map;
    }

    /** Ekstrak semua raw link dari dokumen (href/src). */
    protected List<String> extractRawLinks(JsoupFetcher.Page pr) {
        return pr.document == null ? List.of() : fetcher.extractLinks(pr.document);
    }

    /** Cek & emit BrokenLink jika status bukan 2xx; update total link count. */
    protected void checkAndEmit(URI target, URI sourceCanonical, Map<URI, String> anchorMap) {
        // Unik: cek sekali saja
        if (!linkCatalog.addIfAbsent(target)) return;

        int code = headChecker.check(target, timeoutMillis, userAgent);
        // total links = ukuran katalog (unik)
        service.emitTotalLinks(linkCatalog.size());

        // Broken: I/O (0) atau non-2xx
        if (code == 0 || code < 200 || code >= 400) {
            service.emitBrokenLink(new BrokenLink(
                    target.toString(),
                    code,
                    anchorMap.getOrDefault(target, ""),
                    sourceCanonical.toString()
            ));
        }
    }

    /** Emit satu baris WebpageLink untuk halaman kanonik. */
    protected void emitWebpageRow(URI canonical, JsoupFetcher.Page pr, int outCount) {
        PageRepository.Meta meta = pageRepo.getOrCreate(canonical);
        meta.outlinkCount = outCount;
        service.emitWebpage(new WebpageLink(
                canonical.toString(),
                pr.statusCode,
                outCount,
                pr.fetchedAt
        ));
    }

    /**
     * Record occurrence & (opsional) enqueue halaman same-host yang baru.
     * Subclass bisa override {@link #enqueue(URI)} untuk menambah ke queue/stack; DFS rekursif bisa tidak pakai ini.
     */
    protected void processTargetLink(URI seed, URI canonicalPage, URI target) {
        occurrenceIndex.add(target, canonicalPage);
        if (classifier.isSameHostWebpage(seed, target) && !pageRepo.contains(target)) {
            pageRepo.getOrCreate(target);
            enqueue(target);
        }
    }

    /** Hook untuk BFS (queue) — DFS rekursif boleh abaikan. */
    protected void enqueue(URI uri) {
        // default: no-op; BFS akan override
    }

    // ===================== Helper record =====================

    /** Bundle hasil fetch + URL kanoniknya. */
    protected record CanonicalPage(URI canonical, JsoupFetcher.Page page) {}

    // ===================== Guard util opsional =====================

    /** Cek apakah depth melebihi limit (0 = unlimited). */
    protected boolean overDepth(int depth) {
        return maxDepth > 0 && depth > maxDepth;
    }

    /** Cek apakah limit halaman tercapai (0 = unlimited). */
    protected boolean overPageLimit() {
        return maxPages > 0 && pageRepo.size() >= maxPages;
    }
}
