package dev.claimsrag.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.claimsrag.service.AskResult;
import dev.claimsrag.service.AskService;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Runs the golden set through the real AskService and scores each case with deterministic checks (plus optional judge). */
public class EvalRunner {

    private final AskService service;
    private final ObjectMapper mapper;
    private final LlmJudge judge; // null = skip

    public EvalRunner(AskService service, ObjectMapper mapper, LlmJudge judge) {
        this.service = service;
        this.mapper = mapper;
        this.judge = judge;
    }

    public List<GoldenCase> loadGolden() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/eval/golden.json")) {
            return mapper.readValue(in, new TypeReference<>() {});
        }
    }

    public EvalReport run(List<GoldenCase> golden) {
        List<CaseResult> results = new ArrayList<>();
        double cost = 0;
        for (GoldenCase g : golden) {
            AskResult r = service.ask(g.question());
            cost += r.costUsd();
            results.add(score(g, r));
        }
        return aggregate(results, cost);
    }

    CaseResult score(GoldenCase g, AskResult r) {
        Set<String> retrievedDocs = r.retrieved().stream().map(x -> docOf(x.chunkId())).collect(Collectors.toSet());
        Set<String> retrievedIds = r.retrieved().stream().map(AskResult.Retrieved::chunkId).collect(Collectors.toSet());
        Set<String> citedDocs = r.citations().stream().map(EvalRunner::docOf).collect(Collectors.toSet());

        String ctx = r.retrieved().stream().map(AskResult.Retrieved::text).collect(Collectors.joining("\n")).toLowerCase();
        boolean ctxFacts = g.answerable() && !g.mustContain().isEmpty()
                && g.mustContain().stream().allMatch(f -> ctx.contains(f.toLowerCase()));
        boolean hit = g.expectedDocs().stream().anyMatch(retrievedDocs::contains);
        String lower = r.answer().toLowerCase();
        boolean facts = r.answered() && g.mustContain().stream().allMatch(f -> lower.contains(f.toLowerCase()));
        boolean valid = retrievedIds.containsAll(r.citations()) && (!r.answered() || !r.citations().isEmpty());
        boolean correct = g.expectedDocs().stream().anyMatch(citedDocs::contains);
        boolean abstained = !r.answered();

        Integer judged = null;
        if (judge != null && g.answerable() && r.answered()) {
            judged = judge.score(contextOf(r), g.question(), r.answer());
        }
        return new CaseResult(g.id(), g.answerable(), hit, ctxFacts, facts, valid, correct, abstained, judged, r.answer());
    }

    private static String contextOf(AskResult r) {
        return r.retrieved().stream().map(x -> "[" + x.chunkId() + "] " + x.text()).collect(Collectors.joining("\n"));
    }

    private static String docOf(String chunkId) {
        return chunkId.substring(0, chunkId.indexOf('#'));
    }

    static EvalReport aggregate(List<CaseResult> all, double cost) {
        List<CaseResult> ans = all.stream().filter(CaseResult::answerable).toList();
        List<CaseResult> unans = all.stream().filter(c -> !c.answerable()).toList();
        double ctxRecall = rate(ans, CaseResult::contextHasFacts);
        double judgeAvg = all.stream().filter(c -> c.judgeScore() != null).mapToInt(CaseResult::judgeScore).average().orElse(Double.NaN);
        return new EvalReport(all,
                rate(ans, CaseResult::retrievalHit),
                ctxRecall,
                rate(ans, CaseResult::factsPresent),
                rate(ans, CaseResult::citationsValid),
                rate(ans, CaseResult::citationsCorrect),
                rate(unans, CaseResult::abstainedCorrectly),
                rate(all, CaseResult::passed),
                Double.isNaN(judgeAvg) ? null : judgeAvg,
                cost);
    }

    private static double rate(List<CaseResult> xs, java.util.function.Predicate<CaseResult> p) {
        return xs.isEmpty() ? 1.0 : (double) xs.stream().filter(p).count() / xs.size();
    }
}
