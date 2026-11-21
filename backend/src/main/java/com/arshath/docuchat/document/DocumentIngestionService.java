package com.arshath.docuchat.document;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.arshath.docuchat.config.DocuChatProperties;
import com.arshath.docuchat.config.VectorStoreConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * The "R" in RAG: PDF → one document per page → ~400-token chunks → embeddings → vector store.
 * Every chunk is tagged with its document id and page number so answers can cite sources.
 */
@Service
public class DocumentIngestionService {

    public static final String DOC_ID = "docId";
    public static final String PAGE = "page";

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};

    private final SimpleVectorStore vectorStore;
    private final DocumentRegistry registry;
    private final DocuChatProperties properties;
    private final Clock clock;
    private final TokenTextSplitter splitter = new TokenTextSplitter(400, 200, 20, 2000, true);

    public DocumentIngestionService(SimpleVectorStore vectorStore, DocumentRegistry registry,
                                    DocuChatProperties properties, Clock clock) {
        this.vectorStore = vectorStore;
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
    }

    public DocumentInfo ingest(String filename, byte[] content) {
        if (content.length < PDF_MAGIC.length || !startsWithPdfMagic(content)) {
            throw new InvalidDocumentException("Only PDF files are supported.");
        }

        List<Document> pages = readPages(filename, content);
        if (pages.size() > properties.limits().maxPages()) {
            throw new InvalidDocumentException(
                    "This demo accepts PDFs of up to " + properties.limits().maxPages() + " pages.");
        }

        String docId = UUID.randomUUID().toString();
        List<Document> tagged = new ArrayList<>();
        for (Document page : pages) {
            if (page.getText() == null || page.getText().isBlank()) {
                continue;
            }
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(DOC_ID, docId);
            metadata.put(PAGE, pageNumber(page));
            metadata.put("filename", filename);
            tagged.add(new Document(page.getText(), metadata));
        }
        if (tagged.isEmpty()) {
            throw new InvalidDocumentException(
                    "No selectable text found. Scanned PDFs need OCR first.");
        }

        List<Document> chunks = splitter.apply(tagged);
        if (chunks.isEmpty()) {
            throw new InvalidDocumentException("This PDF doesn't contain enough text to answer questions about.");
        }
        vectorStore.add(chunks);
        persistVectors();

        DocumentInfo info = new DocumentInfo(docId, filename, pages.size(), chunks.size(),
                Instant.now(clock), chunks.stream().map(Document::getId).toList());
        registry.put(info);
        log.info("Ingested '{}' ({} pages, {} chunks) as {}", filename, pages.size(), chunks.size(), docId);
        return info;
    }

    public void delete(String docId) {
        DocumentInfo removed = registry.remove(docId).orElseThrow(() -> new DocumentNotFoundException(docId));
        vectorStore.delete(removed.chunkIds());
        persistVectors();
    }

    /** Public demo: forget documents after the configured TTL so storage and data don't pile up. */
    @Scheduled(fixedDelayString = "PT15M")
    public void evictExpired() {
        Instant cutoff = Instant.now(clock).minus(properties.limits().documentTtl());
        registry.all().stream()
                .filter(d -> d.uploadedAt().isBefore(cutoff))
                .forEach(d -> {
                    delete(d.id());
                    log.info("Evicted expired document {}", d.id());
                });
    }

    private List<Document> readPages(String filename, byte[] content) {
        try {
            ByteArrayResource resource = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPagesPerDocument(1)
                    .build();
            return new PagePdfDocumentReader(resource, config).get();
        } catch (RuntimeException e) {
            throw new InvalidDocumentException("Could not read this PDF. Is it encrypted or corrupted?", e);
        }
    }

    private static int pageNumber(Document page) {
        Object value = page.getMetadata().get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER);
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static boolean startsWithPdfMagic(byte[] content) {
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (content[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private void persistVectors() {
        File file = VectorStoreConfig.vectorFile(properties);
        try {
            java.nio.file.Files.createDirectories(file.toPath().getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory", e);
        }
        vectorStore.save(file);
    }
}
