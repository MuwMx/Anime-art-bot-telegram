package com.example.animeartbot;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GoogleCustomSearchClient {
    private static final String API_KEY = "AIzaSyDUGLziQxm-Kket-Zjmz3v86tWEh9O0I6U";  // Замініть на ваш API ключ
    private static final String GOOGLE_SEARCH_URL = "https://www.googleapis.com/customsearch/v1";
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Random random = new Random();

    public List<Result> searchAnimeArt(String query, String site) throws IOException {
        int startIndex = 1 + random.nextInt(100);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(GOOGLE_SEARCH_URL).newBuilder();
        urlBuilder.addQueryParameter("key", API_KEY);
        urlBuilder.addQueryParameter("cx", site);
        urlBuilder.addQueryParameter("q", query);
        urlBuilder.addQueryParameter("searchType", "image");
        urlBuilder.addQueryParameter("start", String.valueOf(startIndex)); // Додайте параметр start

        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();

        List<Result> results = new ArrayList<>();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                JSONObject jsonObject = new JSONObject(responseBody.string());
                JSONArray items = jsonObject.getJSONArray("items");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String imageUrl = item.getString("link");
                    String originalLink = item.getJSONObject("image").getString("contextLink");
                    results.add(new Result(imageUrl, originalLink));
                }
            }
        }

        return results;
    }

    public static class Result {
        private final String imageUrl;
        private final String originalLink;

        public Result(String imageUrl, String originalLink) {
            this.imageUrl = imageUrl;
            this.originalLink = originalLink;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getOriginalLink() {
            return originalLink;
        }
    }
}
