package com.unpar.skripsi.crawler;

import com.unpar.skripsi.Service;
import com.unpar.skripsi.model.WebpageLink;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

/**
 * Breadth-First crawler (iteratif, FIFO queue).
 * Menggunakan util di superclass {@link Crawler} untuk:
 * - Normalisasi & kanonisasi URL
 * - Fetch & extract links
 * - Cek link unik & emit hasil
 * - Emit skeleton Webpage segera setelah fetch agar urutan BFS terlihat di UI
 */
public class BfsCrawler extends Crawler {

    private final ArrayDeque<URI> queue = new ArrayDeque<>();

    public BfsCrawler(Service service,
                      String userAgent,
                      int timeoutMillis,
                      boolean followRedirects,
                      int maxDepth,
                      int maxPages) {
        super(service, userAgent, timeoutMillis, followRedirects, maxDepth, maxPages);
    }

    @Override
    public void crawl(String seedUrl) {
        final URI seed;
        try {
            seed = URI.create(seedUrl);
        } catch (Exception e) {
            service.fail("Invalid seed URL");
            return;
        }

        // Seed
        pageRepo.getOrCreate(seed);
        enqueue(seed);

        while (service.isRunning() && !queue.isEmpty()) {
            URI page = queue.poll();
            if (page == null) continue;

            // Fetch & canonicalize
            CanonicalPage cp = fetchCanonical(page);
            URI canonicalPage = cp.canonical();
            var pr = cp.page();

            // Skip duplicate canonical or over limit (juga set meta status/visited)
            if (preparePageAndMaybeSkip(canonicalPage, pr, page)) continue;

            // === (1) Emit skeleton row segera setelah fetch ===
            WebpageLink wpRow = new WebpageLink(
                    canonicalPage.toString(),
                    pr.statusCode,
                    0,              // temporary; akan di-update setelah hitung outlinks
                    pr.fetchedAt
            );
            service.emitWebpage(wpRow);

            // Bangun anchor map
            Map<URI, String> anchorMap = buildAnchorMap(canonicalPage, pr);

            // Proses semua link
            List<String> rawLinks = extractRawLinks(pr);
            int outCount = 0;

            for (String raw : rawLinks) {
                if (!service.isRunning()) break;

                URI target = normalizer.normalize(canonicalPage, raw);
                if (target == null) continue;
                outCount++;

                // Record occurrence & enqueue same-host page baru (BFS)
                processTargetLink(seed, canonicalPage, target);

                // Cek link unik & emit broken jika perlu
                checkAndEmit(target, canonicalPage, anchorMap);
            }

            // === (2) Update row yg sama (UI auto-refresh via properties) ===
            pageRepo.getOrCreate(canonicalPage).outlinkCount = outCount;
            wpRow.setLinkCount(outCount);

            // (Tidak perlu) emitWebpageRow(canonicalPage, pr, outCount);
        }

        if (service.isRunning()) {
            service.complete();
        }
    }

    @Override
    protected void enqueue(URI uri) {
        queue.add(uri); // FIFO
    }
}
