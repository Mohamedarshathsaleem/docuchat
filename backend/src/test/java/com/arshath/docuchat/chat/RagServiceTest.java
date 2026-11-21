package com.arshath.docuchat.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Clock;

import com.arshath.docuchat.config.DocuChatProperties;
import com.arshath.docuchat.document.DocumentIngestionService;
import com.arshath.docuchat.document.DocumentInfo;
import com.arshath.docuchat.document.DocumentNotFoundException;
import com.arshath.docuchat.document.DocumentRegistry;
import com.arshath.docuchat.support.FakeChatModel;
import com.arshath.docuchat.support.FakeEmbeddingModel;
import com.arshath.docuchat.support.TestPdfs;
import com.arshath.docuchat.support.TestProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SimpleVectorStore;

class RagServiceTest {

    @TempDir
    Path dir;

    private FakeChatModel chatModel;
    private DocumentIngestionService ingestion;
    private RagService rag;

    @BeforeEach
    void setUp() {
        DocuChatProperties properties = TestProperties.in(dir);
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(new FakeEmbeddingModel()).build();
        DocumentRegistry registry = new DocumentRegistry(properties);
        ingestion = new DocumentIngestionService(vectorStore, registry, properties, Clock.systemUTC());
        chatModel = new FakeChatModel("Refunds are allowed within thirty days [p. 1].");
        rag = new RagService(ChatClient.builder(chatModel), vectorStore, registry, properties);
    }

    @Test
    void answersFromRetrievedChunksAndReturnsPageSources() throws Exception {
        DocumentInfo doc = ingestion.ingest("policy.pdf", TestPdfs.withPages(
                "Refund policy: refunds are allowed within thirty days of purchase.",
                "Warranty covers manufacturing defects for one year."));

        ChatAnswer answer = rag.ask(doc.id(), "What is the refund policy?");

        assertThat(answer.answer()).contains("thirty days");
        assertThat(answer.sources()).isNotEmpty();
        assertThat(answer.sources().get(0).page()).isEqualTo(1);
        assertThat(answer.sources().get(0).excerpt()).contains("Refund policy");
    }

    @Test
    void sendsNumberedPageLabelledContextAndGroundingRules() throws Exception {
        DocumentInfo doc = ingestion.ingest("policy.pdf",
                TestPdfs.withPages("Refund policy: refunds are allowed within thirty days."));

        rag.ask(doc.id(), "refund policy?");

        String prompt = chatModel.prompts().get(0).getContents();
        assertThat(prompt).contains("[1] (page 1)", "refunds are allowed", "Question: refund policy?");
        assertThat(prompt).contains("Use ONLY the numbered context passages");
    }

    @Test
    void doesNotCallTheModelWhenNothingRelevantIsFound() throws Exception {
        DocumentInfo doc = ingestion.ingest("policy.pdf",
                TestPdfs.withPages("Refund policy: refunds are allowed within thirty days."));

        ChatAnswer answer = rag.ask(doc.id(), "quantum chromodynamics lattice gluons");

        assertThat(answer.answer()).isEqualTo(RagService.NOT_FOUND);
        assertThat(answer.sources()).isEmpty();
        assertThat(chatModel.prompts()).isEmpty();
    }

    @Test
    void onlyRetrievesFromTheRequestedDocument() throws Exception {
        DocumentInfo refunds = ingestion.ingest("a.pdf", TestPdfs.withPages("Refund policy thirty days."));
        ingestion.ingest("b.pdf", TestPdfs.withPages("Refund policy ninety days for premium members."));

        ChatAnswer answer = rag.ask(refunds.id(), "refund policy days");

        assertThat(answer.sources()).allSatisfy(s -> assertThat(s.excerpt()).doesNotContain("ninety"));
    }

    @Test
    void unknownDocumentIsRejected() {
        assertThatThrownBy(() -> rag.ask("missing", "anything"))
                .isInstanceOf(DocumentNotFoundException.class);
    }
}
