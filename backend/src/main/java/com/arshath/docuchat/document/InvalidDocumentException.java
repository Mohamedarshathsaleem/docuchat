package com.arshath.docuchat.document;

/** Thrown when an upload is not a readable PDF or breaks the demo limits. */
public class InvalidDocumentException extends RuntimeException {

    public InvalidDocumentException(String message) {
        super(message);
    }

    public InvalidDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
