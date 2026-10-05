package dev.claimsrag.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.claimsrag.config.AssistantProperties;
import dev.claimsrag.llm.LlmClient;
import dev.claimsrag.service.AskService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Run with: mvn spring-boot:run -Dspring-boot.run.arguments="--assistant.eval=true --spring.main.web-application-type=none"
 * Add --assistant.provider=claude to evaluate the real model (needs ANTHROPIC_API_KEY, spends money).
 */
@Component
@ConditionalOnProperty(name = "assistant.eval", havingValue = "true")
public class EvalCommand implements ApplicationRunner {

    private static final double MIN_PASS_RATE = 0.9;

    private final AskService service;
    private final ObjectMapper mapper;
    private final LlmClient llm;
    private final AssistantProperties props;

    public EvalCommand(AskService service, ObjectMapper mapper, LlmClient llm, AssistantProperties props) {
        this.service = service;
        this.mapper = mapper;
        this.llm = llm;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LlmJudge judge = "claude".equalsIgnoreCase(props.provider()) ? new LlmJudge(llm, props) : null;
        EvalRunner runner = new EvalRunner(service, mapper, judge);
        EvalReport report = runner.run(runner.loadGolden());
        String md = report.toMarkdown();
        System.out.println(md);
        Files.createDirectories(Path.of("eval-report"));
        Files.writeString(Path.of("eval-report/report.md"), md);
        if (report.passRate() < MIN_PASS_RATE) {
            System.err.printf("pass rate %.0f%% below threshold %.0f%%%n", report.passRate() * 100, MIN_PASS_RATE * 100);
            System.exit(1);
        }
    }
}
