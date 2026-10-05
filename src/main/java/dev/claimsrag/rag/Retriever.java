package dev.claimsrag.rag;

import java.util.List;

public class Retriever {

    private final Embedder embedder;
    private final VectorStore store;
    private final int topK;
    private final double minScore;

    public Retriever(Embedder embedder, VectorStore store, int topK, double minScore) {
        this.embedder = embedder;
        this.store = store;
        this.topK = topK;
        this.minScore = minScore;
    }

    public List<ScoredChunk> retrieve(String question) {
        return store.search(embedder.embed(question), topK, minScore);
    }
}
