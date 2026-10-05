package dev.claimsrag.llm;

public interface LlmClient {
    LlmResponse complete(LlmRequest request);
}
