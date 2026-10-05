package dev.claimsrag.cost;

import dev.claimsrag.config.AssistantProperties;
import dev.claimsrag.llm.Usage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Converts token usage to USD with a configured price table and keeps running totals per model. */
@Component
public class CostTracker {

    public record Totals(long requests, Usage usage, double costUsd) {}

    private final Map<String, AssistantProperties.Price> pricing;
    private final Map<String, Totals> totals = new ConcurrentHashMap<>();

    public CostTracker(AssistantProperties props) {
        this.pricing = props.pricing() == null ? Map.of() : props.pricing();
    }

    public double costOf(String model, Usage u) {
        AssistantProperties.Price p = pricing.get(model);
        if (p == null) return 0.0;
        return (u.inputTokens() * p.inputPerMillion()
                + u.outputTokens() * p.outputPerMillion()
                + u.cacheReadTokens() * p.cacheReadPerMillion()
                + u.cacheWriteTokens() * p.cacheWritePerMillion()) / 1_000_000.0;
    }

    public double record(String model, Usage u) {
        double cost = costOf(model, u);
        totals.merge(model, new Totals(1, u, cost),
                (a, b) -> new Totals(a.requests() + 1, a.usage().plus(b.usage()), a.costUsd() + b.costUsd()));
        return cost;
    }

    public Map<String, Totals> snapshot() {
        return Map.copyOf(totals);
    }
}
