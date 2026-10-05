package dev.claimsrag.web;

import dev.claimsrag.cost.CostTracker;
import dev.claimsrag.llm.LlmException;
import dev.claimsrag.service.AskResult;
import dev.claimsrag.service.AskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AskController {

    public record AskRequest(@NotBlank @Size(max = 2000) String question) {}

    private final AskService service;
    private final CostTracker costs;

    public AskController(AskService service, CostTracker costs) {
        this.service = service;
        this.costs = costs;
    }

    @PostMapping("/ask")
    public AskResult ask(@Valid @RequestBody AskRequest req) {
        return service.ask(req.question());
    }

    @GetMapping("/usage")
    public Map<String, CostTracker.Totals> usage() {
        return costs.snapshot();
    }

    @ExceptionHandler(LlmException.class)
    ResponseEntity<Map<String, String>> llmFailure(LlmException e) {
        HttpStatus status = e.retryable() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
