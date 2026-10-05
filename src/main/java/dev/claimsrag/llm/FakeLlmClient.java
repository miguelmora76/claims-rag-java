package dev.claimsrag.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic offline stand-in for an LLM so tests and CI run with no network or key.
 * It is extractive: it picks the context sentence that best overlaps the question and cites its chunk.
 * It exercises the pipeline (retrieval, prompt, citations, cost tracking). It says nothing about real model quality.
 */
public class FakeLlmClient implements LlmClient {

    private static final Pattern CHUNK = Pattern.compile("\\[(\\S+?)\\]\\s*(.*?)(?=\\n\\[\\S+?\\]|</context>)", Pattern.DOTALL);
    private static final Pattern QUESTION = Pattern.compile("<question>(.*?)</question>", Pattern.DOTALL);
    private static final Set<String> STOP = Set.of("the", "a", "an", "of", "to", "is", "are", "for", "and", "in", "on",
            "what", "how", "does", "do", "my", "i", "it", "be", "can", "or", "by", "with", "that", "this", "when", "if");

    @Override
    public LlmResponse complete(LlmRequest req) {
        String user = req.user();
        Matcher q = QUESTION.matcher(user);
        String question = q.find() ? q.group(1) : user;
        Set<String> qTokens = tokens(question);

        double best = 0;
        String bestSentence = null;
        String bestId = null;
        Matcher c = CHUNK.matcher(user.substring(Math.max(0, user.indexOf("<context>"))));
        while (c.find()) {
            for (String sentence : c.group(2).split("(?<=\\.)\\s+")) {
                Set<String> st = tokens(sentence);
                if (st.isEmpty()) continue;
                long overlap = qTokens.stream().filter(st::contains).count();
                double score = (double) overlap / Math.max(1, qTokens.size());
                if (score > best) {
                    best = score;
                    bestSentence = sentence.trim();
                    bestId = c.group(1);
                }
            }
        }
        String text = (bestSentence == null || best < 0.34)
                ? "NOT_IN_CONTEXT"
                : bestSentence + " [" + bestId + "]";
        Usage usage = new Usage(estimate(req.system()) + estimate(req.user()), estimate(text), 0, 0);
        return new LlmResponse(text, usage, "fake", 0);
    }

    private static long estimate(String s) {
        return Math.max(1, s.length() / 4);
    }

    private static Set<String> tokens(String s) {
        List<String> out = new ArrayList<>();
        for (String t : s.toLowerCase().split("[^a-z0-9\\-]+")) {
            if (t.length() > 1 && !STOP.contains(t)) out.add(t);
        }
        return Set.copyOf(out);
    }
}
