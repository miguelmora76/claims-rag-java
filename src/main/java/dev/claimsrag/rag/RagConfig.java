package dev.claimsrag.rag;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import dev.claimsrag.config.AssistantProperties;
import dev.claimsrag.llm.ClaudeLlmClient;
import dev.claimsrag.llm.FakeLlmClient;
import dev.claimsrag.llm.LlmClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class RagConfig {

    @Bean
    HashingEmbedder embedder(List<Chunk> corpusChunks) {
        HashingEmbedder e = new HashingEmbedder(512);
        e.fit(corpusChunks.stream().map(c -> c.title() + " " + c.text()).toList());
        return e;
    }

    @Bean
    List<Chunk> corpusChunks() throws IOException {
        Chunker chunker = new Chunker(300);
        List<Chunk> chunks = new ArrayList<>();
        for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath:corpus/*.md")) {
            String docId = r.getFilename().replaceAll("\\.md$", "");
            String text = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            chunks.addAll(chunker.chunk(docId, text));
        }
        return chunks;
    }

    @Bean
    VectorStore vectorStore(Embedder embedder, List<Chunk> corpusChunks) {
        VectorStore store = new VectorStore();
        for (Chunk c : corpusChunks) store.add(c, embedder.embed(c.title() + " " + c.text()));
        return store;
    }

    @Bean
    Retriever retriever(Embedder embedder, VectorStore store, AssistantProperties props) {
        return new Retriever(embedder, store, props.topK(), props.minScore());
    }

    @Bean
    LlmClient llmClient(AssistantProperties props) {
        if ("claude".equalsIgnoreCase(props.provider())) {
            // Reads ANTHROPIC_API_KEY. User-scoped keys (sk-ant-usr-...) also need ANTHROPIC_WORKSPACE_ID.
            var builder = AnthropicOkHttpClient.builder().fromEnv();
            String workspace = System.getenv("ANTHROPIC_WORKSPACE_ID");
            if (workspace != null && !workspace.isBlank()) {
                builder.putHeader("anthropic-workspace-id", workspace);
            }
            AnthropicClient client = builder.build();
            return new ClaudeLlmClient(client);
        }
        return new FakeLlmClient();
    }
}
