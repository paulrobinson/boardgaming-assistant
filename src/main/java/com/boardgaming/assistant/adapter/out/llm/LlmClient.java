package com.boardgaming.assistant.adapter.out.llm;

@FunctionalInterface
public interface LlmClient {
    String complete(String systemPrompt, String userPrompt);
}
