package dev.claimsrag.eval;

import dev.claimsrag.config.AssistantProperties;
import dev.claimsrag.llm.LlmClient;
import dev.claimsrag.llm.LlmRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** LLM-as-judge groundedness score (1-5). Only meaningful with a real model, so the offline run skips it. */
public class LlmJudge {

    private static final Pattern SCORE = Pattern.compile("\"score\"\\s*:\\s*([1-5])");
    private static final String SYSTEM = """
            You grade answers from a retrieval-augmented assistant. Given a context, a question and an answer,
            score how well every claim in the answer is supported by the context:
            5 = fully supported, 3 = partly supported, 1 = unsupported or contradicts the context.
            Reply with JSON only: {"score": <1-5>, "reason": "<short>"}
            """;

    private final LlmClient llm;
    private final AssistantProperties props;

    public LlmJudge(LlmClient llm, AssistantProperties props) {
        this.llm = llm;
        this.props = props;
    }

    public Integer score(String context, String question, String answer) {
        String user = "<context>\n" + context + "\n</context>\n<question>" + question + "</question>\n<answer>" + answer + "</answer>";
        String reply = llm.complete(new LlmRequest(props.judgeModel(), SYSTEM, user, 200, null)).text();
        Matcher m = SCORE.matcher(reply);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
