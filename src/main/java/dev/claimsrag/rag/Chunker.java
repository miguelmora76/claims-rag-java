package dev.claimsrag.rag;

import java.util.ArrayList;
import java.util.List;

/** Splits a document into paragraph-based chunks, merging short paragraphs up to maxChars. */
public class Chunker {

    private final int maxChars;

    public Chunker(int maxChars) {
        this.maxChars = maxChars;
    }

    public List<Chunk> chunk(String docId, String markdown) {
        String title = docId;
        List<String> paragraphs = new ArrayList<>();
        for (String p : markdown.split("\\n\\s*\\n")) {
            String t = p.strip();
            if (t.isEmpty()) continue;
            if (t.startsWith("# ")) {
                String[] lines = t.split("\\n", 2);
                title = lines[0].substring(2).strip();
                t = lines.length > 1 ? lines[1].strip() : "";
                if (t.isEmpty()) continue;
            }
            paragraphs.add(t);
        }
        List<Chunk> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String p : paragraphs) {
            if (cur.length() > 0 && cur.length() + p.length() + 1 > maxChars) {
                out.add(new Chunk(docId + "#" + out.size(), docId, title, cur.toString()));
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append('\n');
            cur.append(p);
        }
        if (cur.length() > 0) out.add(new Chunk(docId + "#" + out.size(), docId, title, cur.toString()));
        return out;
    }
}
