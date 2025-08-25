package com.unpar.webcrawler.models;

public final class BrokenLink extends Link {
    private String anchorText;
    private String webpageUrl;

    public BrokenLink(String url, int code, String anchorText, String webpageUrl) {
        super(url, code);
        this.anchorText = anchorText;
        this.webpageUrl = webpageUrl;
    }

    // ===================== Getter & Setter anchorText =====================

    public String getAnchorText() {
        return anchorText;
    }

    public void setAnchorText(String value) {
        this.anchorText = value;
    }

    // ===================== Getter & Setter webpageUrl =====================

    public String getWebpageUrl() {
        return webpageUrl;
    }

    public void setWebpageUrl(String value) {
        this.webpageUrl = value;
    }
}
