package com.arshath.docuchat.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank String documentId,
        @NotBlank @Size(max = 1000) String question) {
}
