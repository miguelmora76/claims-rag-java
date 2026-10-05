package dev.claimsrag.eval;

public record CaseResult(
        String id,
        boolean answerable,
        boolean retrievalHit,
        boolean contextHasFacts,
        boolean factsPresent,
        boolean citationsValid,
        boolean citationsCorrect,
        boolean abstainedCorrectly,
        Integer judgeScore,
        String answer) {

    /** A case passes when every check that applies to it passes. */
    public boolean passed() {
        if (!answerable) return abstainedCorrectly;
        return retrievalHit && contextHasFacts && factsPresent && citationsValid && citationsCorrect;
    }
}
