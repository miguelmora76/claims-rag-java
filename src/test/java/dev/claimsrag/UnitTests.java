package dev.claimsrag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.claimsrag.config.AssistantProperties;
import dev.claimsrag.cost.CostTracker;
import dev.claimsrag.llm.Usage;
import dev.claimsrag.privacy.PhiRedactor;
import dev.claimsrag.rag.Chunker;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UnitTests {

    @Test
    void chunkerUsesTitleAndStableIds() {
        var chunks = new Chunker(40).chunk("doc", "# My Title\n\nFirst paragraph here.\n\nSecond paragraph is a bit longer than the first.\n");
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).id()).isEqualTo("doc#0");
        assertThat(chunks.get(0).title()).isEqualTo("My Title");
        assertThat(chunks.get(0).text()).doesNotContain("# My Title");
    }

    @Test
    void redactorMasksCommonIdentifiers() {
        String out = PhiRedactor.redact("Patient 123-45-6789 call 555-123-4567 or a.b@example.com, DOB 1/2/1980");
        assertThat(out).contains("[SSN]", "[PHONE]", "[EMAIL]", "[DATE]")
                .doesNotContain("123-45-6789", "555-123-4567", "example.com");
    }

    @Test
    void costTrackerAppliesPriceTableAndAccumulates() {
        var price = new AssistantProperties.Price(4.0, 20.0, 0.2, 5.0);
        var props = new AssistantProperties("fake", "m", "j", 100, null, 3, 0.1, Map.of("m", price));
        var tracker = new CostTracker(props);

        double c = tracker.record("m", new Usage(1_000_000, 100_000, 0, 0));
        assertThat(c).isEqualTo(4.0 + 2.0);
        tracker.record("m", new Usage(0, 50_000, 1_000_000, 0));

        var t = tracker.snapshot().get("m");
        assertThat(t.requests()).isEqualTo(2);
        assertThat(t.costUsd()).isEqualTo(6.0 + 1.0 + 0.2);
        assertThat(tracker.costOf("unknown-model", new Usage(5, 5, 0, 0))).isZero();
    }
}
