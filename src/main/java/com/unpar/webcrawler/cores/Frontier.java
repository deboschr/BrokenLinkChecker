package com.unpar.webcrawler.cores;

import java.util.ArrayDeque;
import java.util.Deque;

public class Frontier {

    private final Deque<String> urls = new ArrayDeque<>();

    public void add(String url) {
        this.urls.addLast(url);
    }

    public String next() {

        if (urls.isEmpty()) return null;

        return this.urls.pollFirst(); // FIFO
    }


    public boolean isEmpty() {
        return urls.isEmpty();
    }
}
