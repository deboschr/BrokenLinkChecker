package com.unpar.webcrawler.cores;

import com.unpar.webcrawler.models.Algorithm;
import java.util.ArrayDeque;
import java.util.Deque;

public class Frontier {

    private final Deque<String> urls = new ArrayDeque<>();
    private final Algorithm algorithm;

    public Frontier(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public void add(String url) {
        this.urls.addLast(url);
    }

    public String next() {

        if (urls.isEmpty()) return null;

        return switch (algorithm) {
            case BFS -> urls.pollFirst(); // FIFO
            case DFS -> urls.pollLast();  // LIFO
        };
    }


    public boolean isEmpty() {
        return urls.isEmpty();
    }
}
