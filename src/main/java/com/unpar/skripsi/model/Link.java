package com.unpar.skripsi.model;

import com.unpar.skripsi.util.HttpStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;

public abstract class Link {
    private final StringProperty url = new SimpleStringProperty();
    private final IntegerProperty statusCode = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();

    protected Link(String url, int code) {
        this.url.set(url);
        this.statusCode.set(code);

        // status selalu mengikuti statusCode
        this.status.bind(Bindings.createStringBinding(
                () -> HttpStatus.getStatus(this.statusCode.get()),
                this.statusCode
        ));
    }

    // ===================== Getter & Setter url =====================

    public String getUrl() {
        return url.get();
    }

    public void setUrl(String value) {
        url.set(value);
    }

    public StringProperty urlProperty() {
        return url;
    }


    // ===================== Getter & Setter statusCode =====================
    public int getStatusCode() {
        return statusCode.get();
    }

    public void setStatusCode(int value) {
        statusCode.set(value);
    }

    public IntegerProperty statusCodeProperty() {
        return statusCode;
    }


    // ===================== Getter & Setter status =====================

    public String getStatus() {
        return status.get();
    }

    public void setStatus(int value) {
        status.set(HttpStatus.getStatus(value));
    }

    public StringProperty statusProperty() {
        return status;
    }

}
