package com.arshath.docuchat.document;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String id) {
        super("No document with id " + id);
    }
}
