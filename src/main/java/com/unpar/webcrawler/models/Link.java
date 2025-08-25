package com.unpar.webcrawler.models;

import com.unpar.webcrawler.utils.HttpStatus;

public abstract class Link {
    private String url;
    private String statusCode;

    protected Link(String url, int statusCode) {
        this.url = url;
        this.statusCode = HttpStatus.getStatus(statusCode);
    }

    // ===================== Getter & Setter url =====================

    public String getUrl() {
        return url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    // ===================== Getter & Setter statusCode =====================

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int value) {
        this.statusCode = HttpStatus.getStatus(value);
    }
}
