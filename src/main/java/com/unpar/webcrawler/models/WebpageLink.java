package com.unpar.webcrawler.models;

import java.time.Instant;

public final class WebpageLink extends Link {

    private int linkCount;
    private Instant accessTime;

    public WebpageLink(String url, int statusCode, int linkCount, Instant accessTime) {
        super(url, statusCode);
        this.linkCount = linkCount;
        this.accessTime = accessTime;
    }

    // ===================== Getter & Setter linkCount =====================

    public int getLinkCount() {
        return linkCount;
    }

    public void setLinkCount(int value) {
        this.linkCount = value;
    }

    // ===================== Getter & Setter accessTime =====================

    public Instant getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(Instant value) {
        this.accessTime = value;
    }
}
