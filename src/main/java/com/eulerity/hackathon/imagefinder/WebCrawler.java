package com.eulerity.hackathon.imagefinder;

import java.io.IOException;
import java.util.List;

public interface WebCrawler {
    List<String> getImageUrls(String url, int depth) throws IOException;
}
