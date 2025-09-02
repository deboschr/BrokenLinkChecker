package com.unpar.webcrawler;

import com.unpar.webcrawler.cores.Crawler;
import com.unpar.webcrawler.models.WebpageLink;
import com.unpar.webcrawler.models.BrokenLink;

import java.util.*;
import java.util.function.Consumer;

public class Application {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print(">> Masukkan Seed URL   : ");
        String seedUrl = scanner.nextLine().trim();


        Crawler crawler = new Crawler(seedUrl);

        List<WebpageLink> webpageLinks = new ArrayList<>();
        List<BrokenLink> brokenLinks   = new ArrayList<>();

        Consumer<WebpageLink> conWebpageLink = new Consumer<WebpageLink>() {
            @Override
            public void accept(WebpageLink wl) {
                System.out.println("=== WebpageLink ===");
                System.out.println("URL             : " + wl.getUrl());
                System.out.println("Status Code     : " + wl.getStatusCode());
                System.out.println("Links Count     : " + wl.getLinkCount());
                System.out.println("Access Time ISO : " + wl.getAccessTime());
                System.out.println();
                webpageLinks.add(wl);
            }
        };

        Consumer<BrokenLink> conBrokenLink = new Consumer<BrokenLink>() {
            @Override
            public void accept(BrokenLink bl) {
                System.out.println("=== BrokenLink ===");
                System.out.println("URL          : " + bl.getUrl());
                System.out.println("Status       : " + bl.getStatusCode());
                System.out.println("Anchor Text  : " + bl.getAnchorText());
                System.out.println("Source Page  : " + bl.getWebpageUrl());
                System.out.println();
                brokenLinks.add(bl);
            }
        };

        crawler.crawl(conWebpageLink, conBrokenLink);

        // ringkasan akhir
        System.out.println("=== RINGKASAN ===");
        System.out.println("Total Webpage Links     : " + webpageLinks.size());
        System.out.println("Total Broken Links      : " + brokenLinks.size());
    }
}
