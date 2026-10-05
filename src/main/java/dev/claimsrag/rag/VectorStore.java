package dev.claimsrag.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Brute-force in-memory cosine search. Fine for a few hundred chunks; use a real vector DB beyond that. */
public class VectorStore {

    private final List<Chunk> chunks = new ArrayList<>();
    private final List<float[]> vectors = new ArrayList<>();

    public void add(Chunk chunk, float[] vector) {
        chunks.add(chunk);
        vectors.add(vector);
    }

    public int size() {
        return chunks.size();
    }

    public List<ScoredChunk> search(float[] query, int k, double minScore) {
        List<ScoredChunk> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            double s = dot(query, vectors.get(i));
            if (s >= minScore) scored.add(new ScoredChunk(chunks.get(i), s));
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return scored.size() > k ? scored.subList(0, k) : scored;
    }

    private static double dot(float[] a, float[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }
}
