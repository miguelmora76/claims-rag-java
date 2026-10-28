package dev.claimsrag.cost;

import dev.claimsrag.config.AssistantProperties;
import dev.claimsrag.llm.Usage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Converts token usage to USD with a configured price table and records it as Micrometer counters tagged by model.
 * The process keeps no totals of its own: scrape /actuator/prometheus and sum across instances for fleet-wide numbers.
 */
@Component
public class CostTracker {

    public record Totals(long requests, Usage usage, double costUsd) {}

    private static final String REQUESTS = "assistant.llm.requests";
    private static final String TOKENS = "assistant.llm.tokens";
    private static final String COST = "assistant.llm.cost.usd";

    private final Map<String, AssistantProperties.Price> pricing;
    private final MeterRegistry registry;

    public CostTracker(AssistantProperties props, MeterRegistry registry) {
        this.pricing = props.pricing() == null ? Map.of() : props.pricing();
        this.registry = registry;
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
        Counter.builder(REQUESTS).tag("model", model).register(registry).increment();
        Counter.builder(COST).tag("model", model).register(registry).increment(cost);
        tokens(model, "input", u.inputTokens());
        tokens(model, "output", u.outputTokens());
        tokens(model, "cache_read", u.cacheReadTokens());
        tokens(model, "cache_write", u.cacheWriteTokens());
        return cost;
    }

    private void tokens(String model, String type, long n) {
        Counter.builder(TOKENS).tag("model", model).tag("type", type).register(registry).increment(n);
    }

    /** This instance's totals since it started, read back from the meter registry. */
    public Map<String, Totals> snapshot() {
        Map<String, Totals> out = new HashMap<>();
        for (Counter c : registry.find(REQUESTS).counters()) {
            String model = c.getId().getTag("model");
            out.put(model, new Totals(
                    (long) c.count(),
                    new Usage(count(TOKENS, model, "input"), count(TOKENS, model, "output"),
                            count(TOKENS, model, "cache_read"), count(TOKENS, model, "cache_write")),
                    registry.find(COST).tag("model", model).counter().count()));
        }
        return Map.copyOf(out);
    }

    private long count(String name, String model, String type) {
        Counter c = registry.find(name).tag("model", model).tag("type", type).counter();
        return c == null ? 0 : (long) c.count();
    }
}
