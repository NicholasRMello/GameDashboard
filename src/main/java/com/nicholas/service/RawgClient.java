package com.nicholas.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Client to fetch game data via the RAWG API.
 */
public class RawgClient {
    //  RAWG API key
    private static final String API_KEY = "b6808ae0578941ceaf7a14bf766bd5b5";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    /**
     * Data returned by the API: average game time and image URL.
     */
    public record GameInfo(double playtime, String imageUrl) {}

    /**
     * Only fetches the average playtime (in hours) for a title.
     */
    public static Optional<Double> fetchPlaytime(String title) {
        return fetchGameInfo(title).map(GameInfo::playtime);
    }

    /**
     * Searches for playtime and cover URL for a title.
     * @param title title of the game to search for
     * @return Optional with GameInfo(playtime, imageUrl) or empty if not found
     */
    public static Optional<GameInfo> fetchGameInfo(String title) {
        try {
            String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String uri = String.format(
                    "https://api.rawg.io/api/games?search=%s&key=%s", encoded, API_KEY
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            JSONObject root = new JSONObject(response.body());
            JSONArray results = root.optJSONArray("results");
            if (results != null && results.length() > 0) {
                JSONObject first = results.getJSONObject(0);
                double hours = first.optDouble("playtime", 0);
                String img = first.optString("background_image", "");
                return Optional.of(new GameInfo(hours, img));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<GameDetails> fetchGameDetails(String title) {
        try {
            // 1) first search the game and takes the ID
            String searchUri = String.format(
                    "https://api.rawg.io/api/games?search=%s&key=%s",
                    URLEncoder.encode(title, StandardCharsets.UTF_8),
                    API_KEY
            );
            HttpResponse<String> searchResp = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder()
                                    .uri(URI.create(searchUri))
                                    .header("Accept","application/json")
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            JSONObject root = new JSONObject(searchResp.body());
            JSONArray results = root.optJSONArray("results");
            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }

            long gameId = results.getJSONObject(0).getLong("id");

            // 2) Now load full details
            String detailUri = String.format(
                    "https://api.rawg.io/api/games/%d?key=%s",
                    gameId, API_KEY
            );
            HttpResponse<String> detailResp = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder()
                                    .uri(URI.create(detailUri))
                                    .header("Accept","application/json")
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            JSONObject det = new JSONObject(detailResp.body());

            // 3) Extract fields
            String name     = det.optString("name", "");
            String released = det.optString("released", "");
            double rating   = det.optDouble("rating", 0.0);
            String bgImg    = det.optString("background_image", "");
            String desc     = det.optString("description_raw", "");

            // 4) Genres come as array of objects { "name": "Action" }
            List<String> genres = det.optJSONArray("genres")
                    .toList()
                    .stream()
                    .map(o -> ((Map<?,?>)o).get("name").toString())
                    .collect(Collectors.toList());

            return Optional.of(new GameDetails(
                    gameId, name, released, rating, genres, bgImg, desc
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }


    public record GameDetails(
            long id,
            String name,
            String released,
            double rating,
            List<String> genres,
            String backgroundImage,
            String description
    ) {}
}

