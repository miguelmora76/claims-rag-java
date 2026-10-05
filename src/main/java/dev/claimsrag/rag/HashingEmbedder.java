package dev.claimsrag.rag;

import java.util.Set;

/**
 * Local, dependency-free embedder: hashed bag of unigrams and bigrams, L2-normalised.
 * It is lexical, not semantic. Swap in a real embedding model behind {@link Embedder} for semantic search.
 */
public class HashingEmbedder implements Embedder {

    private static final Set<String> STOP = Set.of("the", "a", "an", "of", "to", "is", "are", "for", "and", "in", "on",
            "what", "how", "does", "do", "my", "i", "it", "be", "can", "or", "by", "with", "that", "this", "when", "if");
    private static final java.util.regex.Pattern CODE = java.util.regex.Pattern.compile("[a-z]{1,3}-\\d+");
    private final int dims;
    private float[] idf; // per-bucket inverse document frequency; null until fit() is called

    public HashingEmbedder(int dims) {
        this.dims = dims;
    }

    /** Learns IDF weights from the corpus so rare terms (like a denial code) outweigh common ones. */
    public void fit(java.util.List<String> corpus) {
        int[] df = new int[dims];
        for (String doc : corpus) {
            float[] raw = rawCounts(doc);
            for (int i = 0; i < dims; i++) if (raw[i] != 0) df[i]++;
        }
        idf = new float[dims];
        for (int i = 0; i < dims; i++) idf[i] = (float) Math.log(1.0 + (double) corpus.size() / (1 + df[i]));
    }

    @Override
    public float[] embed(String text) {
        float[] v = rawCounts(text);
        if (idf != null) for (int i = 0; i < dims; i++) v[i] *= idf[i];
        double norm = 0;
        for (float x : v) norm += x * x;
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < v.length; i++) v[i] /= (float) norm;
        return v;
    }

    private float[] rawCounts(String text) {
        float[] v = new float[dims];
        String prev = null;
        for (String token : text.toLowerCase().split("[^a-z0-9\\-]+")) {
            if (token.length() < 2 || STOP.contains(token)) continue;
            String raw = stem(token);
            add(v, raw, CODE.matcher(raw).matches() ? 3f : 1f); // denial codes like co-27 are strong exact-match signals
            if (prev != null) add(v, prev + "_" + raw, 0.5f);
            prev = raw;
        }
        return v;
    }

    /** Crude suffix stripping so "decided", "decides" and "decision" land on the same feature. */
    static String stem(String t) {
        for (String suffix : new String[] {"ions", "ion", "ing", "ed", "es", "s"}) {
            if (t.length() > suffix.length() + 3 && t.endsWith(suffix)) return t.substring(0, t.length() - suffix.length());
        }
        return t;
    }

    private void add(float[] v, String feature, float weight) {
        int h = feature.hashCode();
        int idx = Math.floorMod(h, dims);
        v[idx] += ((h >>> 31) == 0 ? 1f : -1f) * weight;
    }
}
