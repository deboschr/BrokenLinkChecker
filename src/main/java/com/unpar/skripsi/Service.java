package com.unpar.skripsi;

import com.unpar.skripsi.crawler.BfsCrawler;
import com.unpar.skripsi.crawler.DfsCrawler;
import com.unpar.skripsi.crawler.UrlNormalizer;
import com.unpar.skripsi.model.Algorithm;
import com.unpar.skripsi.model.BrokenLink;
import com.unpar.skripsi.model.WebpageLink;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Orchestrates crawling and streams results back to the Controller via callbacks.
 * Notes:
 * - This class NEVER calls Platform.runLater(). The Controller must marshal to the UI thread.
 * - Actual BFS/DFS crawling is delegated to classes in the crawler/ package.
 */
public class Service {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    // ===== Runtime configuration (expose setters as needed) =====
    private int httpTimeoutMillis = 10000;
    private String userAgent = "BrokenLinkChecker/1.0";
    private boolean followRedirects = true;

    // Crawl limits (0 = unlimited). Highly recommended to set for DFS (recursive).
    private int maxDepth = 4;   // default: safe depth for DFS; BFS will simply ignore if not used
    private int maxPages = 0;   // unlimited by default

    // You can use this later if you implement parallel fetching.
    private int parallelism = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);

    // ===== Executor & state =====
    private ExecutorService crawlExecutor;
    private volatile boolean running;

    // ===== Callbacks (no-op defaults) =====
    private Consumer<BrokenLink> onBrokenLink = bl -> {};
    private Consumer<WebpageLink> onWebpage = wp -> {};
    private IntConsumer onTotalLinks = total -> {};
    private Runnable onComplete = () -> {};
    private Consumer<String> onError = msg -> {};

    // ===== Public API =====

    /** Start crawling with enum Algorithm. */
    public synchronized void startCrawling(String seedUrl, Algorithm algorithm) {
        Objects.requireNonNull(seedUrl, "seedUrl cannot be null");
        Objects.requireNonNull(algorithm, "algorithm cannot be null");

        if (running) {
            onError.accept("Crawler is already running.");
            return;
        }

        // Normalize seed upfront (adds trailing '/', lowercases host, etc.)
        var seedUri = normalizer.normalizeAbsolute(seedUrl);
        if (seedUri == null) {
            onError.accept("Invalid seed URL");
            stopInternal();
            return;
        }
        final String normalizedSeed = seedUri.toString();

        running = true;

        // single worker for now; adjust if you parallelize later
        crawlExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "crawl-worker");
            t.setDaemon(true);
            return t;
        });

        crawlExecutor.submit(() -> {
            try {
                switch (algorithm) {
                    case BFS -> new BfsCrawler(
                            this, userAgent, httpTimeoutMillis, followRedirects,
                            /*maxDepth*/ maxDepth, /*maxPages*/ maxPages
                    ).crawl(normalizedSeed);

                    case DFS -> new DfsCrawler(
                            this, userAgent, httpTimeoutMillis, followRedirects,
                            /*maxDepth*/ maxDepth, /*maxPages*/ maxPages
                    ).crawl(normalizedSeed);
                }
            } catch (Throwable t) {
                if (running) onError.accept(t.getMessage());
            } finally {
                stopInternal();
            }
        });
    }

    /** Backward-compatible: start from a user-facing label (e.g., "Breadth-First Search (BFS)"). */
    public void startCrawling(String seedUrl, String algorithmLabel) {
        startCrawling(seedUrl, Algorithm.fromLabel(algorithmLabel));
    }

    /** Request a graceful stop. Crawlers should check isRunning(). */
    public synchronized void stop() { stopInternal(); }

    private synchronized void stopInternal() {
        running = false;
        if (crawlExecutor != null) {
            crawlExecutor.shutdownNow();
            crawlExecutor = null;
        }
    }

    // ===== Emit helpers (to be called by crawlers) =====

    /** Emit a broken link occurrence (source page is inside the model). */
    public void emitBrokenLink(BrokenLink bl) {
        if (!running || bl == null) return;
        onBrokenLink.accept(bl);
    }

    /** Emit a crawled same-host webpage row. */
    public void emitWebpage(WebpageLink wp) {
        if (!running || wp == null) return;
        onWebpage.accept(wp);
    }

    /** Emit total links checked so far (accumulated). */
    public void emitTotalLinks(int total) {
        if (!running) return;
        onTotalLinks.accept(total);
    }

    /** Signal normal completion. */
    public void complete() {
        if (!running) return;
        onComplete.run();
        stopInternal();
    }

    /** Signal fatal error. */
    public void fail(String message) {
        if (!running) return;
        onError.accept(message == null ? "Unknown error" : message);
        stopInternal();
    }

    // ===== Callback setters =====
    public void setOnBrokenLink(Consumer<BrokenLink> cb) { this.onBrokenLink = (cb != null ? cb : bl -> {}); }
    public void setOnWebpage(Consumer<WebpageLink> cb) { this.onWebpage = (cb != null ? cb : wp -> {}); }
    public void setOnTotalLinkUpdate(IntConsumer cb) { this.onTotalLinks = (cb != null ? cb : total -> {}); }
    public void setOnComplete(Runnable cb) { this.onComplete = (cb != null ? cb : () -> {}); }
    public void setOnError(Consumer<String> cb) { this.onError = (cb != null ? cb : msg -> {}); }

    // ===== Config setters/getters =====
    public void setHttpTimeoutMillis(int v) { this.httpTimeoutMillis = v; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public void setFollowRedirects(boolean v) { this.followRedirects = v; }
    public void setParallelism(int v) { this.parallelism = Math.max(1, v); }

    /** Depth/page limits (0 = unlimited). Recommended: keep DFS maxDepth around 3–6. */
    public void setMaxDepth(int v) { this.maxDepth = Math.max(0, v); }
    public void setMaxPages(int v) { this.maxPages = Math.max(0, v); }

    public int getHttpTimeoutMillis() { return httpTimeoutMillis; }
    public String getUserAgent() { return userAgent; }
    public boolean isFollowRedirects() { return followRedirects; }
    public int getParallelism() { return parallelism; }
    public int getMaxDepth() { return maxDepth; }
    public int getMaxPages() { return maxPages; }
    public boolean isRunning() { return running; }
}
