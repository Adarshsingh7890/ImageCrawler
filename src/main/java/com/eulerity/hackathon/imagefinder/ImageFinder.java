package com.eulerity.hackathon.imagefinder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@WebServlet(name = "ImageFinder", urlPatterns = {"/main"})
public class ImageFinder extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().create();

    private WebCrawler webCrawler = new JsoupWebCrawler();

    // Setter for dependency injection
    public void setWebCrawler(WebCrawler webCrawler) {
        this.webCrawler = webCrawler;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        
        String url = req.getParameter("url");
        int depth = 2; 

        if (url == null || url.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"URL parameter is required.\"}");
            return;
        }

        try {
            List<String> imageUrls = webCrawler.getImageUrls(url, depth);
            resp.getWriter().write(GSON.toJson(imageUrls));
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"An error occurred while processing the URL.\"}");
        } finally {
            resp.getWriter().flush(); // Ensure the response is fully sent
        }
    }
}
