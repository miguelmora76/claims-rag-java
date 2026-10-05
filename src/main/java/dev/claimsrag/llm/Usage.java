package dev.claimsrag.llm;

public record Usage(long inputTokens, long outputTokens, long cacheReadTokens, long cacheWriteTokens) {
    public static final Usage ZERO = new Usage(0, 0, 0, 0);

    public Usage plus(Usage o) {
        return new Usage(inputTokens + o.inputTokens, outputTokens + o.outputTokens,
                cacheReadTokens + o.cacheReadTokens, cacheWriteTokens + o.cacheWriteTokens);
    }
}
