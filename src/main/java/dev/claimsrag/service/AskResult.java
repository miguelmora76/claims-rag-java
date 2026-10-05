package dev.claimsrag.service;

import dev.claimsrag.llm.Usage;
import java.util.List;

public record AskResult(
        String answer,
        boolean answered,
        List<String> citations,
        List<Retrieved> retrieved,
        Usage usage,
        double costUsd,
        long latencyMs) {

    public record Retrieved(String chunkId, String title, double score) {}
}
