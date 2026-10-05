package dev.claimsrag.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * provider: "fake" (offline, deterministic) or "claude" (needs ANTHROPIC_API_KEY).
 * pricing: USD per 1M tokens, keyed by model id. Check current prices before trusting cost output.
 */
@ConfigurationProperties(prefix = "assistant")
public record AssistantProperties(
        String provider,
        String model,
        String judgeModel,
        long maxTokens,
        String effort,
        int topK,
        double minScore,
        Map<String, Price> pricing) {

    public record Price(double inputPerMillion, double outputPerMillion,
                        double cacheReadPerMillion, double cacheWritePerMillion) {}
}
