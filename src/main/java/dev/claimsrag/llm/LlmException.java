package dev.claimsrag.llm;

public class LlmException extends RuntimeException {
    private final boolean retryable;

    public LlmException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
