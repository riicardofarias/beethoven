package br.com.tdsoft.beethoven.common;

import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import static java.util.Optional.ofNullable;

@Component
@Order(-2)
public class CustomErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public CustomErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer codecConfigurer) {
        super(errorAttributes, new Resources(), applicationContext);
        super.setMessageWriters(codecConfigurer.getWriters());
        super.setMessageReaders(codecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        var status = getStatus(request);
        var error = getError(request);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(
                        ExceptionResponse.builder()
                                .message(getMessage(error))
                                .status(status)
                                .build())
                );
    }

    private String getMessage(Throwable error) {
        if (error instanceof WebExchangeBindException ex) {
            return ex.getAllErrors().toString();
        }
        return ofNullable(error.getCause())
                .flatMap(it -> ofNullable(it.getMessage()))
                .orElse(error.getMessage());
    }

    private HttpStatus getStatus(ServerRequest request) {
        return HttpStatus.resolve((Integer) getErrorAttributes(request, ErrorAttributeOptions.defaults()).get("status"));
    }
}
