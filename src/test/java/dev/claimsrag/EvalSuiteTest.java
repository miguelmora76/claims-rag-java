package dev.claimsrag;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.claimsrag.eval.EvalReport;
import dev.claimsrag.eval.EvalRunner;
import dev.claimsrag.service.AskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Offline eval gate: runs the golden set against the pipeline with the deterministic fake model.
 * It guards retrieval, prompt assembly, citation handling and abstention against regressions.
 * Model-quality evals run separately with --assistant.provider=claude (see README).
 */
@SpringBootTest(properties = "assistant.provider=fake")
class EvalSuiteTest {

    @Autowired AskService service;
    @Autowired ObjectMapper mapper;

    @Test
    void goldenSetMeetsThresholds() throws Exception {
        EvalRunner runner = new EvalRunner(service, mapper, null);
        EvalReport report = runner.run(runner.loadGolden());
        System.out.println(report.toMarkdown());

        // Fact coverage and pass rate are not gated here: the fake model is extractive and often picks the wrong
        // sentence from correct context. They are meaningful only in the live run.
        assertThat(report.retrievalRecall()).as("retrieval recall").isGreaterThanOrEqualTo(0.95);
        assertThat(report.citationValidity()).as("citation validity").isEqualTo(1.0);
        assertThat(report.abstainAccuracy()).as("abstain accuracy").isEqualTo(1.0);
    }
}
