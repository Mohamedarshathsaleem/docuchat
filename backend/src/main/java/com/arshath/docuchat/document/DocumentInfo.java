package com.arshath.docuchat.document;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** A PDF that has been split into chunks and embedded. */
public record DocumentInfo(
        String id,
        String filename,
        int pages,
        int chunks,
        Instant uploadedAt,
        @JsonIgnore List<String> chunkIds) {
}
