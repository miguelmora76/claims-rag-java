package dev.claimsrag.llm;

/** Provider-neutral request. effort may be null. */
public record LlmRequest(String model, String system, String user, long maxTokens, String effort) {}
