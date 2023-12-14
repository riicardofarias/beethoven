package br.com.tdsoft.beethoven.common;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Value
@Builder
@Jacksonized
public class ExceptionResponse {
    @Builder.Default
    LocalDateTime date = LocalDateTime.now();
    String message;
    HttpStatus status;
}
