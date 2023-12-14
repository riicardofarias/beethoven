package br.com.tdsoft.beethoven.common;

import lombok.Getter;

@Getter
public class MaxAttemptException extends RuntimeException {
    private final int attempt;

    public MaxAttemptException(int attempt) {
        super("Failed to process message, maximum retries reached (" + attempt + ")");
        this.attempt = attempt;
    }
}
