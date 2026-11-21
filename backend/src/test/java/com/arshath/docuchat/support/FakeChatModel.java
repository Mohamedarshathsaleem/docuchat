package com.arshath.docuchat.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/** Records every prompt and replies with a fixed answer. */
public class FakeChatModel implements ChatModel {

    private final String reply;
    private final List<Prompt> prompts = new ArrayList<>();

    public FakeChatModel(String reply) {
        this.reply = reply;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
    }

    public List<Prompt> prompts() {
        return prompts;
    }
}
