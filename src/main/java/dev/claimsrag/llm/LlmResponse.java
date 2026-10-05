package dev.claimsrag.llm;

public record LlmResponse(String text, Usage usage, String model, long latencyMs) {}
