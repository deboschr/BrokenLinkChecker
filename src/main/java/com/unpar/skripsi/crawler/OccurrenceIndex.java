package com.unpar.skripsi.crawler;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OccurrenceIndex {

    // target -> ordered set of sources
    private final Map<URI, Set<URI>> targetToSources = new ConcurrentHashMap<>();

    /** Catat bahwa 'source' menaut ke 'target'. */
    public void add(URI target, URI source) {
        if (target == null || source == null) return;
        targetToSources
                .computeIfAbsent(target, k -> Collections.synchronizedSet(new LinkedHashSet<>()))
                .add(source);
    }

    /** Ambil sumber-sumber untuk sebuah target (read-only copy). */
    public List<URI> getSources(URI target) {
        Set<URI> set = targetToSources.get(target);
        if (set == null) return List.of();
        synchronized (set) {
            return List.copyOf(set);
        }
    }

    /** Berapa banyak sumber yang menunjuk ke target ini? */
    public int count(URI target) {
        Set<URI> set = targetToSources.get(target);
        return set == null ? 0 : set.size();
    }

    /** Semua target yang tercatat. */
    public Set<URI> allTargets() {
        return Collections.unmodifiableSet(targetToSources.keySet());
    }

    public int size() {
        return targetToSources.size();
    }

    public void clear() {
        targetToSources.clear();
    }
}
