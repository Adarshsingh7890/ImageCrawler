package com.eulerity.hackathon.imagefinder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ImageFinderTest {

    private ImageFinder servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;
    private WebCrawler webCrawler;

    // Used Mockito for mocking the servlet, request and response objects.

    @Before
    public void setUp() throws Exception {
        servlet = new ImageFinder();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        webCrawler = Mockito.mock(WebCrawler.class);
        
        responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        Mockito.when(response.getWriter()).thenReturn(printWriter);
        
        servlet.setWebCrawler(webCrawler);
    }
    // example.com is just a placeholder.

    @Test
    public void testDoPostWithValidUrlAndDepth() throws IOException, ServletException, InterruptedException {
        String url = "http://example.com";
        int depth = 2;
        Mockito.when(request.getParameter("url")).thenReturn(url);

        List<String> fakeImageUrls = Arrays.asList("http://example.com/image1.jpg", "http://example.com/image2.jpg");
        Mockito.when(webCrawler.getImageUrls(url, depth)).thenReturn(fakeImageUrls);

        servlet.doPost(request, response);

        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("http://example.com/image1.jpg"));
        assertTrue(jsonResponse.contains("http://example.com/image2.jpg"));
    }


    @Test
    public void testDoPostWithEmptyUrl() throws IOException, ServletException {
        Mockito.when(request.getParameter("url")).thenReturn("");

        servlet.doPost(request, response);

        String jsonResponse = responseWriter.toString();
        assertTrue(jsonResponse.contains("error"));
        assertTrue(jsonResponse.contains("URL parameter is required"));
    }
}
