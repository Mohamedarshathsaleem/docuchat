package com.arshath.docuchat.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.arshath.docuchat.config.DocuChatProperties;
import com.arshath.docuchat.support.FakeEmbeddingModel;
import com.arshath.docuchat.support.TestPdfs;
import com.arshath.docuchat.support.TestProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;

class DocumentIngestionServiceTest {

    @TempDir
    Path dir;

    private SimpleVectorStore vectorStore;
    private DocumentRegistry registry;
    private DocumentIngestionService service;
    private DocuChatProperties properties;

    @BeforeEach
    void setUp() {
        properties = TestProperties.in(dir);
        vectorStore = SimpleVectorStore.builder(new FakeEmbeddingModel()).build();
        registry = new DocumentRegistry(properties);
        service = new DocumentIngestionService(vectorStore, registry, properties, Clock.systemUTC());
    }

    @Test
    void ingestsEachPageAndTagsChunksWithDocumentAndPage() throws Exception {
        byte[] pdf = TestPdfs.withPages(
                "Refunds are available within thirty days of purchase.",
                "Shipping to Malaysia takes five working days.");

        DocumentInfo info = service.ingest("policy.pdf", pdf);

        assertThat(info.pages()).isEqualTo(2);
        assertThat(info.chunks()).isGreaterThanOrEqualTo(2);
        assertThat(registry.find(info.id())).isPresent();

        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder().query("shipping Malaysia working days").topK(1).build());
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getMetadata())
                .containsEntry(DocumentIngestionService.DOC_ID, info.id())
                .containsEntry(DocumentIngestionService.PAGE, 2);
    }

    @Test
    void persistsVectorsAndRegistryToDisk() throws Exception {
        service.ingest("a.pdf", TestPdfs.withPages("Persistent storage test page."));

        assertThat(dir.resolve("vector-store.json")).exists();
        assertThat(new DocumentRegistry(properties).list()).hasSize(1);
    }

    @Test
    void rejectsFilesThatAreNotPdfs() {
        assertThatThrownBy(() -> service.ingest("notes.txt", "hello".getBytes()))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void rejectsPdfsOverThePageLimit() throws Exception {
        byte[] sixPages = TestPdfs.withPages("one", "two", "three", "four", "five", "six");

        assertThatThrownBy(() -> service.ingest("long.pdf", sixPages))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("5 pages");
    }

    @Test
    void deleteRemovesDocumentAndItsChunks() throws Exception {
        DocumentInfo info = service.ingest("a.pdf", TestPdfs.withPages("Delete me please, unique zebra text."));

        service.delete(info.id());

        assertThat(registry.find(info.id())).isEmpty();
        assertThat(vectorStore.similaritySearch(
                SearchRequest.builder().query("unique zebra text").topK(5).build())).isEmpty();
    }

    @Test
    void evictsDocumentsOlderThanTheTtl() throws Exception {
        Instant uploaded = Instant.parse("2026-01-01T00:00:00Z");
        DocumentIngestionService atUpload = new DocumentIngestionService(vectorStore, registry, properties,
                Clock.fixed(uploaded, ZoneOffset.UTC));
        DocumentInfo info = atUpload.ingest("old.pdf", TestPdfs.withPages("An old document that should expire after one day."));

        new DocumentIngestionService(vectorStore, registry, properties,
                Clock.fixed(uploaded.plus(Duration.ofHours(25)), ZoneOffset.UTC)).evictExpired();

        assertThat(registry.find(info.id())).isEmpty();
    }
}
