package com.unpar.skripsi.model;


import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.Instant;

public final class WebpageLink extends Link{

    private final IntegerProperty linkCount = new SimpleIntegerProperty();
    private final ObjectProperty<Instant> accessTime = new SimpleObjectProperty<>();

    public WebpageLink(String url, int statusCode, int linkCount, Instant accessTime) {
        super(url, statusCode);
        this.linkCount.set(linkCount);
        this.accessTime.set(accessTime);
    }


    // ===================== Getter & Setter linkCount =====================

    public int getLinkCount() {
        return linkCount.get();
    }

    public void setLinkCount(int value) {
        linkCount.set(value);
    }

    public IntegerProperty linkCountProperty() {
        return linkCount;
    }

    // ===================== Getter & Setter accessTime =====================

    public Instant getAccessTime() {
        return accessTime.get();
    }

    public void setAccessTime(Instant value) {
        accessTime.set(value);
    }

    public ObjectProperty<Instant> accessTimeProperty() {
        return accessTime;
    }
}
