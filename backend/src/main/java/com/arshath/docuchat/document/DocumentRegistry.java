package com.arshath.docuchat.document;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.arshath.docuchat.config.DocuChatProperties;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.stereotype.Component;

/** Keeps track of uploaded documents and the vector-store chunk ids that belong to each. */
@Component
public class DocumentRegistry {

    private final Map<String, DocumentInfo> documents = new ConcurrentHashMap<>();
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    public DocumentRegistry(DocuChatProperties properties) {
        this.file = properties.storageDir().resolve("documents.json");
        load();
    }

    public void put(DocumentInfo document) {
        documents.put(document.id(), document);
        save();
    }

    public Optional<DocumentInfo> find(String id) {
        return Optional.ofNullable(documents.get(id));
    }

    public List<DocumentInfo> list() {
        return documents.values().stream()
                .sorted(Comparator.comparing(DocumentInfo::uploadedAt).reversed())
                .toList();
    }

    public Collection<DocumentInfo> all() {
        return List.copyOf(documents.values());
    }

    public Optional<DocumentInfo> remove(String id) {
        DocumentInfo removed = documents.remove(id);
        if (removed != null) {
            save();
        }
        return Optional.ofNullable(removed);
    }

    private synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            // chunkIds is @JsonIgnore for the API, so persist a plain map that includes it
            Map<String, StoredDocument> stored = new ConcurrentHashMap<>();
            documents.forEach((id, d) -> stored.put(id, StoredDocument.from(d)));
            mapper.writeValue(file.toFile(), stored);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save document registry", e);
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            Map<String, StoredDocument> stored = mapper.readValue(file.toFile(), new TypeReference<>() { });
            stored.forEach((id, s) -> documents.put(id, s.toDocumentInfo()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read document registry " + file, e);
        }
    }

    /** Persistence shape of {@link DocumentInfo}, including the chunk ids. */
    static final class StoredDocument {
        public String id;
        public String filename;
        public int pages;
        public int chunks;
        public java.time.Instant uploadedAt;
        public List<String> chunkIds;

        static StoredDocument from(DocumentInfo d) {
            StoredDocument s = new StoredDocument();
            s.id = d.id();
            s.filename = d.filename();
            s.pages = d.pages();
            s.chunks = d.chunks();
            s.uploadedAt = d.uploadedAt();
            s.chunkIds = d.chunkIds();
            return s;
        }

        DocumentInfo toDocumentInfo() {
            return new DocumentInfo(id, filename, pages, chunks, uploadedAt, chunkIds);
        }
    }
}
