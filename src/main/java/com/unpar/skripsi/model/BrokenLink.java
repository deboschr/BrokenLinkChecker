package com.unpar.skripsi.model;

import javafx.beans.property.*;


public final class BrokenLink extends Link {
    private final StringProperty anchorText = new SimpleStringProperty();
    private final StringProperty webpageUrl = new SimpleStringProperty();

    public BrokenLink(String url, int statusCode, String anchorText, String webpageUrl) {
        super(url, statusCode);
        this.anchorText.set(anchorText);
        this.webpageUrl.set(webpageUrl);
    }


    // ===================== Getter & Setter anchorText =====================

    public String getAnchorText() {
        return anchorText.get();
    }

    public void setAnchorText(String value) {
        anchorText.set(value);
    }

    public StringProperty anchorTextProperty() {
        return anchorText;
    }

    // ===================== Getter & Setter webpageUrl =====================

    public String getWebpageUrl() {
        return webpageUrl.get();
    }

    public void setWebpageUrl(String value) {
        webpageUrl.set(value);
    }

    public StringProperty webpageUrlProperty() {
        return webpageUrl;
    }
}
