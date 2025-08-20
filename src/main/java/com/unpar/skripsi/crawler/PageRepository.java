package com.unpar.skripsi.crawler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PageRepository {

    public static class Meta {
        public volatile int statusCode;
        public volatile Instant visitedAt;
        public volatile int outlinkCount;
    }

    private final Map<URI, Meta> store = new ConcurrentHashMap<>();

    /** Ada entri untuk URI ini? */
    public boolean contains(URI uri) {
        return store.containsKey(uri);
    }

    /** Ambil meta; buat baru jika belum ada. */
    public Meta getOrCreate(URI uri) {
        return store.computeIfAbsent(uri, k -> new Meta());
    }

    /** Ukuran repository (jumlah halaman diketahui). */
    public int size() {
        return store.size();
    }

    /** Bersihkan. */
    public void clear() {
        store.clear();
    }
}
