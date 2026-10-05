package dev.claimsrag.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import java.util.stream.Collectors;

/** Calls Claude through the official Anthropic Java SDK. The SDK retries 408/409/429/5xx itself. */
public class ClaudeLlmClient implements LlmClient {

    private final AnthropicClient client;

    public ClaudeLlmClient(AnthropicClient client) {
        this.client = client;
    }

    @Override
    public LlmResponse complete(LlmRequest req) {
        MessageCreateParams.Builder b = MessageCreateParams.builder()
                .model(req.model())
                .maxTokens(req.maxTokens())
                .system(req.system())
                .addUserMessage(req.user());
        if (req.effort() != null && !req.effort().isBlank()) {
            b.outputConfig(OutputConfig.builder()
                    .effort(OutputConfig.Effort.of(req.effort().toLowerCase()))
                    .build());
        }
        long start = System.nanoTime();
        Message m;
        try {
            m = client.messages().create(b.build());
        } catch (RateLimitException e) {
            throw new LlmException("Claude rate limited", true, e);
        } catch (AnthropicServiceException e) {
            throw new LlmException("Claude API error " + e.statusCode(), e.statusCode() >= 500, e);
        } catch (AnthropicIoException e) {
            throw new LlmException("Claude connection error", true, e);
        }
        long ms = (System.nanoTime() - start) / 1_000_000;

        String text = m.content().stream()
                .flatMap(block -> block.text().stream())
                .map(t -> t.text())
                .collect(Collectors.joining());
        var u = m.usage();
        Usage usage = new Usage(
                u.inputTokens(),
                u.outputTokens(),
                u.cacheReadInputTokens().orElse(0L),
                u.cacheCreationInputTokens().orElse(0L));
        return new LlmResponse(text, usage, req.model(), ms);
    }
}
