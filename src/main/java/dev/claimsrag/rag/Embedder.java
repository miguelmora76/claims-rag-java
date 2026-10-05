package dev.claimsrag.rag;

public interface Embedder {
    float[] embed(String text);
}
