package com.unpar.skripsi.crawler;

import com.unpar.skripsi.Service;
import com.unpar.skripsi.model.WebpageLink;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Depth-First crawler (rekursif, tanpa Frontier).
 * - Emit skeleton Webpage segera setelah fetch agar urutan DFS (mendalam) terlihat di UI.
 * - Gunakan maxDepth / maxPages agar aman dari eksplorasi berlebihan.
 */
public class DfsCrawler extends Crawler {

    public DfsCrawler(Service service,
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

        pageRepo.getOrCreate(seed);
        dfs(seed, seed, 0);

        if (service.isRunning()) {
            service.complete();
        }
    }

    /** DFS rekursif: proses halaman, lalu telusuri anak same-host yang belum dikunjungi. */
    private void dfs(URI seed, URI page, int depth) {
        if (!service.isRunning()) return;
        if (overDepth(depth)) return;
        if (overPageLimit()) return;

        // Fetch & pilih URL kanonik (hasil redirect + normalisasi)
        CanonicalPage cp = fetchCanonical(page);
        URI canonicalPage = cp.canonical();
        var pr = cp.page();

        // Skip bila kanonik sudah pernah diproses / limit halaman tercapai (serta set meta status/visited)
        if (preparePageAndMaybeSkip(canonicalPage, pr, page)) return;

        System.out.println("ENTER depth=" + depth + " " + canonicalPage);

        // === (1) Emit skeleton row SEGERA (linkCount=0) ===
        WebpageLink wpRow = new WebpageLink(
                canonicalPage.toString(),
                pr.statusCode,
                0,              // sementara 0; di-update setelah proses semua outlinks (termasuk anak-DFS)
                pr.fetchedAt
        );
        service.emitWebpage(wpRow);

        // Build anchor map & ekstrak raw links
        Map<URI, String> anchorMap = buildAnchorMap(canonicalPage, pr);
        List<String> rawLinks = extractRawLinks(pr);

        int outCount = 0;

        for (String raw : rawLinks) {
            if (!service.isRunning()) break;

            URI target = normalizer.normalize(canonicalPage, raw);
            if (target == null) continue;
            outCount++;

            // Catat occurrence (target <- source kanonik)
            occurrenceIndex.add(target, canonicalPage);

            // Rekursi ke same-host webpage yang belum dikunjungi
            if (classifier.isSameHostWebpage(seed, target) && !pageRepo.contains(target)) {
                pageRepo.getOrCreate(target); // tandai diketahui sebelum turun agar tidak diduplikasi

                dfs(seed, target, depth + 1);
            } else {
                // Cek & emit BrokenLink (unik per target)
                checkAndEmit(target, canonicalPage, anchorMap);
            }
        }

        // === (2) Update row yg sama (UI auto-refresh via properties) ===
        pageRepo.getOrCreate(canonicalPage).outlinkCount = outCount;
        wpRow.setLinkCount(outCount);

        System.out.println("LEAVE depth=" + depth + " " + canonicalPage);
    }
}
