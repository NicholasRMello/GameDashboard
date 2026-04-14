package com.nicholas.gamedashboard.api.dto;

import java.util.List;

public record RawgLookupResponse(
    Long rawgId,
    String title,
    String released,
    Double rating,
    List<String> genres,
    String imageUrl,
    String description,
    Double playtime
) {
}
