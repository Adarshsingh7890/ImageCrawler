package com.eulerity.hackathon.imagefinder;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class JsoupWebCrawler implements WebCrawler {

    private List<Thread> threads = new ArrayList<>();
    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();
    private final long REQUEST_INTERVAL_MS = 10;

    private final BlockingQueue<UrlDepthPair> queue = new ArrayBlockingQueue<>(1000);
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final Set<String> uniqueImageUrls = ConcurrentHashMap.newKeySet();
    private volatile int activeCrawlers = 0;  // Count of active crawling threads
    private String baseDomain;  // Base domain of the starting URL

    public JsoupWebCrawler() {
        for (int i = 0; i < MAX_THREADS; i++) {
            Thread thread = new Thread(this::runCrawler, "Crawler-Thread-" + i);
            threads.add(thread);
            thread.start();
        }
    }

    @Override
    public List<String> getImageUrls(String url, int maxDepth) throws IOException {
        this.baseDomain = getBaseDomain(url);  // Set the base domain
        queue.add(new UrlDepthPair(url, 0, maxDepth));
        visited.add(url);

        try {
            waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Properly handle interruption
        }

        System.out.println("Crawling complete. Number of unique images found: " + uniqueImageUrls.size());
        return new ArrayList<>(uniqueImageUrls);
    }

    private String getBaseDomain(String url) throws MalformedURLException {
        URL uri = new URL(url);
        return uri.getHost();  // Returns the domain
    }

    private void waitForCompletion() throws InterruptedException {
        synchronized (this) {
            while (!queue.isEmpty() || activeCrawlers > 0) {
                wait();  // Wait for all threads to finish processing
            }
        }
    }

    private void runCrawler() {
        while (!Thread.currentThread().isInterrupted()) {
            UrlDepthPair current = null;
            try {
                current = queue.take();  // This method throws InterruptedException
                processUrl(current);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Properly handle interruption
                return;
            } catch (Exception e) {
                System.err.println("Error processing URL: " + (current != null ? current.url : "unknown") + ", Error: " + e.getMessage());
            }
        }
    }

    private void processUrl(UrlDepthPair current) {
        try {
            if (current.depth > current.maxDepth) return; // Stop processing if the max depth is exceeded

            Thread.sleep(REQUEST_INTERVAL_MS);  // This method throws InterruptedException

            if (isTextBasedContent(current.url)) {
                Document doc = Jsoup.connect(current.url).get();
                Elements images = doc.select("img[src]");
                for (Element img : images) {
                    String absUrl = img.absUrl("src").split("\\?")[0];
                    uniqueImageUrls.add(absUrl);
                }
                if (current.depth < current.maxDepth) {
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String absLink = link.absUrl("href");
                        if (getBaseDomain(absLink).equals(baseDomain)) {  // Check if the domain matches
                            synchronized (this) {
                                if (visited.add(absLink)) {
                                    queue.add(new UrlDepthPair(absLink, current.depth + 1, current.maxDepth));
                                    System.out.println(Thread.currentThread().getName() + " adds URL to queue: " + absLink);
                                    notifyAll();  // Notify other threads that there are new items in the queue
                                }
                            }
                        }
                    }
                }
            } else {
                uniqueImageUrls.add(current.url); // Directly add the URL if it's an image
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
        } catch (IOException e) {
            System.err.println("IOException encountered while processing URL: " + current.url);
        }
    }

    public void shutdown() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isTextBasedContent(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        String contentType = connection.getContentType();
        return contentType != null && (contentType.startsWith("text/") || contentType.contains("xml"));
    }

    static class UrlDepthPair {
        final String url;
        final int depth;
        final int maxDepth;

        UrlDepthPair(String url, int depth, int maxDepth) {
            this.url = url;
            this.depth = depth;
            this.maxDepth = maxDepth;
        }
    }
}
