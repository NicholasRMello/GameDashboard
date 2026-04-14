package com.nicholas.gamedashboard.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicholas.gamedashboard.api.dto.RawgLookupResponse;

@Service
public class RawgService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    @Value("${rawg.api.base-url:https://api.rawg.io/api}")
    private String rawgBaseUrl;

    @Value("${rawg.api.key:}")
    private String rawgApiKey;

    public RawgService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<RawgLookupResponse> lookup(String title) {
        if (rawgApiKey == null || rawgApiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String searchUrl = String.format("%s/games?search=%s&page_size=1&key=%s", rawgBaseUrl, encodedTitle, rawgApiKey);
            JsonNode firstResult = get(searchUrl).path("results").path(0);
            if (firstResult.isMissingNode()) {
                return Optional.empty();
            }

            long gameId = firstResult.path("id").asLong();
            String detailsUrl = String.format("%s/games/%d?key=%s", rawgBaseUrl, gameId, rawgApiKey);
            JsonNode details = get(detailsUrl);

            List<String> genres = new ArrayList<>();
            for (JsonNode genre : details.path("genres")) {
                genres.add(genre.path("name").asText());
            }

            RawgLookupResponse response = new RawgLookupResponse(
                details.path("id").asLong(),
                details.path("name").asText(),
                details.path("released").asText(null),
                details.path("rating").asDouble(),
                genres,
                details.path("background_image").asText(null),
                details.path("description_raw").asText(""),
                details.path("playtime").asDouble(0)
            );

            return Optional.of(response);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private JsonNode get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }
}
