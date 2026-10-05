package dev.claimsrag.rag;

/** id looks like "timely-filing#2" (document id, chunk index). */
public record Chunk(String id, String docId, String title, String text) {}
