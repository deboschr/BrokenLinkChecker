package com.unpar.skripsi.model;

import javafx.beans.property.*;
import java.time.Duration;
import java.time.Instant;

public class CrawlSummary {

    // ====== Domain fields (bisa dipakai di service/logic) ======
    private ExecutionStatus status = ExecutionStatus.IDLE;
    private int totalWebpages;
    private int totalLinks;
    private int totalBrokenLinks;
    private Instant startedAt;
    private Instant finishedAt;   // null jika belum selesai
    private Duration duration = Duration.ZERO; // final duration saat selesai

    // ====== UI properties (opsional untuk binding ke Label) ======
    private final StringProperty statusText = new SimpleStringProperty("⏳ Not started");
    private final IntegerProperty totalWebpagesProp = new SimpleIntegerProperty(0);
    private final IntegerProperty totalLinksProp = new SimpleIntegerProperty(0);
    private final IntegerProperty totalBrokenLinksProp = new SimpleIntegerProperty(0);
    private final StringProperty durationText = new SimpleStringProperty("00:00");

    /* =================== DOMAIN API =================== */

    public void start(Instant startedAt) {
        this.status = ExecutionStatus.CHECKING;
        this.startedAt = startedAt;
        this.finishedAt = null;
        this.duration = Duration.ZERO;
        applyToUi();
    }

    public void finish(Instant finishedAt) {
        this.status = ExecutionStatus.COMPLETED;
        this.finishedAt = finishedAt;
        this.duration = calcDuration();
        applyToUi();
    }

    public void stop(Instant stoppedAt) {
        this.status = ExecutionStatus.STOPPED;
        this.finishedAt = stoppedAt;
        this.duration = calcDuration();
        applyToUi();
    }

    public void error(Instant at) {
        this.status = ExecutionStatus.ERROR;
        this.finishedAt = at;
        this.duration = calcDuration();
        applyToUi();
    }

    /** Update semua counter sekaligus. */
    public void setCounts(int totalWebpages, int totalLinks, int totalBrokenLinks) {
        this.totalWebpages = totalWebpages;
        this.totalLinks = totalLinks;
        this.totalBrokenLinks = totalBrokenLinks;
        applyToUiCounts();
    }

    /** Dipanggil periodik (mis. tiap 1 detik) saat status CHECKING agar durasi live. */
    public void tick() {
        if (status == ExecutionStatus.CHECKING && startedAt != null) {
            durationText.set(formatDuration(Duration.between(startedAt, Instant.now())));
        }
    }

    /* =================== GETTERS domain =================== */
    public ExecutionStatus getStatus() { return status; }
    public int getTotalWebpages() { return totalWebpages; }
    public int getTotalLinks() { return totalLinks; }
    public int getTotalBrokenLinks() { return totalBrokenLinks; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Duration getDuration() { return duration; }               // final (saat selesai)
    public Duration getEffectiveDuration() { return calcDuration(); } // berjalan/akhir

    /* =================== UI properties =================== */
    public StringProperty statusTextProperty() { return statusText; }
    public IntegerProperty totalWebpagesProperty() { return totalWebpagesProp; }
    public IntegerProperty totalLinksProperty() { return totalLinksProp; }
    public IntegerProperty totalBrokenLinksProperty() { return totalBrokenLinksProp; }
    public StringProperty durationTextProperty() { return durationText; }

    /* =================== Sinkronisasi UI =================== */
    private void applyToUi() {
        statusText.set(switch (status) {
            case IDLE -> "⏳ Not started";
            case CHECKING -> "🔍 Running";
            case COMPLETED -> "✔️ Completed";
            case STOPPED -> "⏹️ Stopped";
            case ERROR -> "❌ Error";
        });
        applyToUiCounts();

        // Durasi final / awal / live
        if (status == ExecutionStatus.CHECKING) {
            // biarkan live lewat tick()
            durationText.set(formatDuration(Duration.between(startedAt, Instant.now())));
        } else {
            durationText.set(formatDuration(calcDuration()));
        }
    }

    private void applyToUiCounts() {
        totalWebpagesProp.set(totalWebpages);
        totalLinksProp.set(totalLinks);
        totalBrokenLinksProp.set(totalBrokenLinks);
    }

    private Duration calcDuration() {
        if (startedAt == null) return Duration.ZERO;
        Instant end = (finishedAt != null) ? finishedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    /* =================== Formatter =================== */
    private static String formatDuration(Duration d) {
        long h = d.toHours();
        int m = d.toMinutesPart();
        int s = d.toSecondsPart();
        return (h > 0) ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s);
    }
}
