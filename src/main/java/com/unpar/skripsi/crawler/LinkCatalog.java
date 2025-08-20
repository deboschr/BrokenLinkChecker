package com.unpar.skripsi.crawler;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LinkCatalog {

    private final Set<URI> unique = ConcurrentHashMap.newKeySet();

    /** Tambahkan jika belum ada; return true jika baru ditambahkan. */
    public boolean addIfAbsent(URI uri) {
        return uri != null && unique.add(uri);
    }

    public boolean contains(URI uri) {
        return uri != null && unique.contains(uri);
    }

    public int size() {
        return unique.size();
    }

    public void clear() {
        unique.clear();
    }
}
