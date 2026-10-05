package dev.claimsrag.eval;

import java.util.List;

public record EvalReport(
        List<CaseResult> cases,
        double retrievalRecall,
        double contextRecall,
        double factCoverage,
        double citationValidity,
        double citationCorrectness,
        double abstainAccuracy,
        double passRate,
        Double avgJudgeScore,
        double totalCostUsd) {

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder("# Eval report\n\n| metric | value |\n|---|---|\n");
        row(sb, "retrieval recall (expected doc in top-k)", retrievalRecall);
        row(sb, "context recall (retrieved text contains the required facts)", contextRecall);
        row(sb, "fact coverage (required facts in answer)", factCoverage);
        row(sb, "citation validity (cites only retrieved chunks)", citationValidity);
        row(sb, "citation correctness (cites expected doc)", citationCorrectness);
        row(sb, "abstain accuracy (declines unanswerable)", abstainAccuracy);
        row(sb, "case pass rate", passRate);
        if (avgJudgeScore != null) sb.append("| judge groundedness (1-5) | ").append(String.format("%.2f", avgJudgeScore)).append(" |\n");
        sb.append("| total cost (USD) | ").append(String.format("%.5f", totalCostUsd)).append(" |\n\n");
        sb.append("## Failures\n\n");
        long failures = cases.stream().filter(c -> !c.passed()).count();
        if (failures == 0) sb.append("none\n");
        for (CaseResult c : cases) {
            if (!c.passed()) sb.append("- `").append(c.id()).append("` [retrieval=").append(c.retrievalHit()).append(" ctxFacts=").append(c.contextHasFacts()).append(" facts=").append(c.factsPresent())
                    .append(" cited=").append(c.citationsCorrect()).append("]: ").append(c.answer().replace('\n', ' ')).append('\n');
        }
        return sb.toString();
    }

    private static void row(StringBuilder sb, String name, double v) {
        sb.append("| ").append(name).append(" | ").append(String.format("%.0f%%", v * 100)).append(" |\n");
    }
}
