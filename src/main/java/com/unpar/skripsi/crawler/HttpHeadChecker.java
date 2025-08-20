package com.unpar.skripsi.crawler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP status checker:
 * - Try HEAD first
 * - Fallback to GET if HEAD not supported (405/501) or on transport error
 * - Returns 0 on I/O/transport failure
 */
public class HttpHeadChecker {

    private static final String DEFAULT_UA = "BrokenLinkChecker/1.0";
    private static final int RETRIES = 1; // small retry for flaky network

    private final HttpClient client;

    public HttpHeadChecker(boolean followRedirects) {
        this.client = HttpClient.newBuilder()
                .followRedirects(followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER)
                .build();
    }

    public int check(URI uri, int timeoutMillis, String userAgent) {
        if (uri == null) return 0;
        final Duration timeout = Duration.ofMillis(Math.max(1, timeoutMillis));
        final String ua = (userAgent == null || userAgent.isBlank()) ? DEFAULT_UA : userAgent;

        // 1) Try HEAD (with tiny retry)
        Integer headCode = tryHead(uri, timeout, ua, RETRIES);
        if (headCode != null) {
            // If server doesn't support HEAD, try GET
            if (headCode == 405 || headCode == 501) {
                Integer getCode = tryGet(uri, timeout, ua, RETRIES);
                return getCode != null ? getCode : 0;
            }
            return headCode;
        }

        // 2) HEAD failed (exception) -> fallback GET
        Integer getCode = tryGet(uri, timeout, ua, RETRIES);
        return getCode != null ? getCode : 0;
    }

    private Integer tryHead(URI uri, Duration timeout, String ua, int retries) {
        int attempts = 0;
        while (true) {
            try {
                HttpRequest req = HttpRequest.newBuilder(uri)
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(timeout)
                        .header("User-Agent", ua)
                        .header("Accept", "*/*")
                        .build();
                HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                return resp.statusCode();
            } catch (Exception e) {
                if (attempts++ >= retries) return null; // give up
                // retry once on network errors
            }
        }
    }

    private Integer tryGet(URI uri, Duration timeout, String ua, int retries) {
        int attempts = 0;
        while (true) {
            try {
                HttpRequest req = HttpRequest.newBuilder(uri)
                        .GET()
                        .timeout(timeout)
                        .header("User-Agent", ua)
                        .header("Accept", "*/*")
                        .build();
                HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                return resp.statusCode();
            } catch (Exception e) {
                if (attempts++ >= retries) return null; // give up
                // retry once on network errors
            }
        }
    }
}
