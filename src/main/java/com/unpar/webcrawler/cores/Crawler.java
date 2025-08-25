package com.unpar.webcrawler.cores;

import com.unpar.webcrawler.models.Algorithm;
import com.unpar.webcrawler.models.BrokenLink;
import com.unpar.webcrawler.models.WebpageLink;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import java.util.function.Consumer;

/**
 * Catatan:
 * - Rate limiting belum ada. Tambahkan delay untuk menghindari ban
 */
public class Crawler {


    private final String rootHost;                          // Untuk host dari seed url
    private final Frontier frontier;                        // Menyimpan daftar webpage link yang akan di crawl
    private final Set<String> repositories;                 // Memastikan semua link unik

    private static final String USER_AGENT = "BrokenLinkChecker/1.0 (+https://github.com/jakeschr/BrokenLinkChecker; contact: 6182001060@student.unpar.ac.id)";
    private static final int TIMEOUT = 10000;
    private static final HttpClient httpClient = HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)    // redirect jika mendapat status code redirect (301,302,303,307,308)
            .connectTimeout(Duration.ofMillis(TIMEOUT))     // mengatur koneksi ke server
            .build();



    public Crawler(String seedUrl, Algorithm algorithm) {
        this.rootHost = URI.create(seedUrl).getHost().toLowerCase();
        this.repositories = new HashSet<>();
        this.frontier = new Frontier(algorithm);

        frontier.add(seedUrl);
    }

    public void crawl(Consumer<WebpageLink> streamWebpageLink,
                      Consumer<BrokenLink> streamBrokenLink) {

        while (!frontier.isEmpty()) {

            String webpageLink = frontier.next();

            if (!repositories.add(webpageLink)) {
                continue;
            }

            Document doc;               // untuk hasil parse html dari webpage
            int wlStatusCode = 0;       // untuk status code dari hasil request ke webpage

            try {

                Connection.Response res = Jsoup
                        .connect(webpageLink)           // buat koneksi ke server
                        .userAgent(USER_AGENT)          // menetapkan user agent
                        .timeout(TIMEOUT)               // menetapkan timeout request
                        .followRedirects(true)       // mengikuti redirect (default : true)
                        .ignoreHttpErrors(true)      // tetap dapat response meski status code 4xx/5xx
                        .execute();                     // kirim request ke server


                // dapatkan status code dar
                wlStatusCode = res.statusCode();

                // jika error
                if (wlStatusCode >= 400 || wlStatusCode == 0) {
                    // Stream hasil
                    streamBrokenLink.accept(new BrokenLink(webpageLink, wlStatusCode, "", ""));
                    continue;
                }

                // dapatkan dokumen html
                doc = res.parse();

                if (doc.selectFirst("html") == null) {
                    continue;
                }

            } catch (Exception e) { // untuk network error, SSL, timeout, dll.
                // Stream hasil
                streamBrokenLink.accept(new BrokenLink(webpageLink, 0, e.getClass().getSimpleName(), ""));
                continue;
            }



            // Ekstrak semua link dari webpage
            List<BrokenLink> linksOnWebpage = extractLinks(doc);

            // Stream hasil
            streamWebpageLink.accept(new WebpageLink(webpageLink, wlStatusCode, linksOnWebpage.size(), Instant.now()));

            for (BrokenLink bl : linksOnWebpage) {

                // jika url berpotensi menjadi webpage
                if (isPotentialWebpage(bl.getUrl())) {

                    // jika link belum di parse maka masukan ke frontier
                    if (!repositories.contains(bl.getUrl())) {
                        frontier.add(bl.getUrl());
                    }
                }
                // jika link bukan webpage
                else {

                    if (!repositories.add(bl.getUrl())) {
                        continue;
                    }

                    // cek link
                    int blStatusCode = fetchUrl(bl.getUrl());

                    // jika error
                    if (blStatusCode >= 400 || blStatusCode == 0) {

                        bl.setStatusCode(blStatusCode);
                        bl.setWebpageUrl(webpageLink);

                        // Stream hasil
                        streamBrokenLink.accept(bl);
                    }
                }
            }
        }
    }

    private List<BrokenLink> extractLinks(Document doc) {
        List<BrokenLink> results = new ArrayList<>();

        for (Element a : doc.select("a[href]")) {

            // Ambil absolut url
            String absoluteUrl = a.attr("abs:href");

            // normalisasi url
            String cleanedUrl = URLCanonicalization(absoluteUrl);

            if (cleanedUrl == null) {
                continue;
            }

            // Mengambil anchor text dari tantan
            String anchorText = a.text().trim();

            results.add(new BrokenLink(cleanedUrl, 0, anchorText, ""));
        }

        return results;
    }

    private boolean isPotentialWebpage(String url) {
        try {
            URI uri = URI.create(url);

            String host = uri.getHost();

            if (host == null) return false;

            // cek same host
            boolean isSameHost = host.equalsIgnoreCase(rootHost);

            String path = uri.getPath().toLowerCase();

            // cek file resource dari path (bukan full URL string)
            boolean isFileResource =
                       path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")
                    || path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg")
                    || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".pdf")
                    || path.endsWith(".zip") || path.endsWith(".rar") || path.endsWith(".7z")
                    || path.endsWith(".mjs") || path.endsWith(".map") || path.endsWith(".json");

            return !isFileResource && isSameHost;
        } catch (Exception e) {
            return false;
        }
    }

    public static String URLCanonicalization(String rawUrl) {

        /** ==================================================================
         * - url input tidak boleh null atau string kosong
         * - buat objek url melalui URI.create() agar kita bisa memecah komponen url
         */

        if (rawUrl == null || rawUrl.trim().isEmpty()) return null;

        URI url;

        try {
            url = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            return rawUrl;
        }

        /** ==================================================================
         * SCHEME
         * - ubah ke lowercase
         * - wajib http/s
         * - tidak boleh null
         */

        String scheme = url.getScheme();

        if (scheme == null) {
            return null;
        }

        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            return null;
        }


        scheme = scheme.toLowerCase();

        /** ==================================================================
         * HOST
         * - ubah ke lowercase
         * - gunakan IDN (Internationalized Domain Name) agar bisa mendapatkan host yang menggunakan karekter non ASCII
         * - tidak boleh null atau kosong (absolut url wajib memiliki host)
         */

        String host = url.getHost();

        if (host == null || host.isEmpty()) {
            return null;
        }

        try {
            host = IDN.toASCII(host).toLowerCase();
        } catch (Exception e) {
            return null;
        }

        /** ==================================================================
         * PORT
         * - hapus port default http  = 80
         * - hapus port default https = 443
         * - ubah port jadi -1 agar tidak bertabrakan dengan nilai port valid
         */

        int port = url.getPort();
        boolean isHttpPortExist = scheme.equals("http") && port == 80;
        boolean isHttpsPortExist = scheme.equals("https") && port == 443;

        if (isHttpPortExist || isHttpsPortExist) {
            port = -1;
        }


        /** ==================================================================
         * PATH
         * - menggunakan getPath() agar langsung menghilangkan dot-segment resolution (/./ dan /../)
         */

        String path = url.getPath();

        if (path == null) {
            path = "";
        }

        /** ==================================================================
         * QUERY
         * - menggunakan getRawQuery() agar mendapatkan query apa adanya
         */

        String query = url.getRawQuery();

        /** ==================================================================
         * REBUILD URL
         * - tidak menyertakan userInfo karena tidak dibutuhkan
         * - tidak menyertakan fragment karena tidak dikirim ke server
         * - kembalikan url yang sudah bersih
         */

        try {
            /**
             * Struktur URL menurut RFC 3986:
             *
             *   https://user:pass@www.example.com:8080/path/to/page.html?id=123&sort=asc#section2
             *
             *   scheme   : "https"
             *   userinfo : "user:pass"
             *   host     : "www.example.com"
             *   port     : 8080
             *   path     : "/path/to/page.html"
             *   query    : "?id=123&sort=asc"
             *   fragment : "#section2"
             */
            URI cleanedUrl = new URI(scheme, null, host, port, path, query, null);

            return cleanedUrl.toASCIIString();

        } catch (URISyntaxException e) {
            return null;
        }
    }

    private int fetchUrl(String url) {
        List<Integer> fallbackStatusCode = Arrays.asList(403, 405, 501, 999);

        try {
            HttpRequest headReq = HttpRequest
                    .newBuilder(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())   // menggunakan method HEAD dan request tanpa body
                    .header("User-Agent", USER_AGENT)                        // mengatur user agent
                    .timeout(Duration.ofMillis(TIMEOUT))                                              // mengatur timeout request
                    .build();

            HttpResponse<Void> headRes = httpClient.send(
                    headReq,                                    // objek request HEAD
                    HttpResponse.BodyHandlers.discarding()      // mengabaikan response body
            );

            int statusCode = headRes.statusCode();                  // ambil status code dari response HEAD

            if (fallbackStatusCode.contains(statusCode)) {
                HttpRequest getReq = HttpRequest
                        .newBuilder(URI.create(url))
                        .method("GET", HttpRequest.BodyPublishers.noBody())    // menggunakan method GET dan request tanpa body
                        .header("User-Agent", USER_AGENT)                        // mengatur user agent
                        .timeout(Duration.ofMillis(TIMEOUT))                                              // mengatur timeout request
                        .build();

                HttpResponse<Void> getRes = httpClient.send(
                        getReq,                                 // objek request GET
                        HttpResponse.BodyHandlers.discarding()  // mengabaikan response body
                );

                statusCode = getRes.statusCode();               // ambil status code dari response GET
            }

            return statusCode;
        } catch (Exception e) {
            return 0;
        }
    }
}