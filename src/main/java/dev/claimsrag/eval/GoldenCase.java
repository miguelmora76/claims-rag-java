package dev.claimsrag.eval;

import java.util.List;

public record GoldenCase(String id, String question, boolean answerable, List<String> expectedDocs, List<String> mustContain) {}
