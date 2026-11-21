package com.arshath.docuchat.chat;

import java.util.List;

/** The model's answer plus the exact chunks it was given, so the UI can show citations. */
public record ChatAnswer(String answer, List<Source> sources) {

    public record Source(int page, String excerpt, double score) {
    }
}
