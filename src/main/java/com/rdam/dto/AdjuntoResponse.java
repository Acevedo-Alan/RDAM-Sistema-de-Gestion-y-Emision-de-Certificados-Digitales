package com.rdam.dto;

import java.time.OffsetDateTime;

public record AdjuntoResponse(
        Integer id,
        Integer solicitudId,
        String nombreOriginal,
        String mimeType,
        Integer tamanoBytes,
        OffsetDateTime createdAt
) {
}
