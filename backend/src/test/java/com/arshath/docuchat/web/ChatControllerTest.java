package com.arshath.docuchat.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.arshath.docuchat.chat.ChatAnswer;
import com.arshath.docuchat.chat.ChatController;
import com.arshath.docuchat.chat.RagService;
import com.arshath.docuchat.config.DocuChatProperties;
import com.arshath.docuchat.document.DocumentNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
@EnableConfigurationProperties(DocuChatProperties.class)
class ChatControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RagService ragService;

    @Test
    void returnsAnswerWithSources() throws Exception {
        given(ragService.ask(eq("doc-1"), any())).willReturn(new ChatAnswer("Thirty days [p. 2].",
                List.of(new ChatAnswer.Source(2, "Refunds within thirty days", 0.82))));

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"doc-1\",\"question\":\"Refunds?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Thirty days [p. 2]."))
                .andExpect(jsonPath("$.sources[0].page").value(2));
    }

    @Test
    void rejectsBlankQuestionWithProblemDetail() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"doc-1\",\"question\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("question must not be blank"));
    }

    @Test
    void unknownDocumentIs404() throws Exception {
        given(ragService.ask(eq("nope"), any())).willThrow(new DocumentNotFoundException("nope"));

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"nope\",\"question\":\"hi\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rateLimitReturns429() throws Exception {
        given(ragService.ask(any(), any())).willReturn(new ChatAnswer("ok", List.of()));
        String body = "{\"documentId\":\"doc-1\",\"question\":\"hi\"}";

        for (int i = 0; i < 20; i++) {
            mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(body)
                    .header("X-Forwarded-For", "203.0.113.9"));
        }

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-Forwarded-For", "203.0.113.9"))
                .andExpect(status().isTooManyRequests());
    }
}
