package com.abax.memory.api.rest.v2;

import com.abax.memory.api.dto.v2.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Exception mapper for Jackson {@link JsonProcessingException} thrown
 * during request body deserialization.
 *
 * <p>Uses {@code @Priority(1)} to take precedence over RESTEasy Reactive
 * built-in deserialization error handling, which produces HTTP 400 with
 * an empty body. This mapper ensures a structured {@link ErrorResponse}
 * is returned instead.</p>
 *
 * <p>This mapper complements {@link V2ExceptionMapper}, which handles
 * {@code JsonProcessingException} found in cause chains. However, when
 * the exception is thrown directly by Jackson during body parsing
 * (before the resource method is dispatched), RESTEasy may route it
 * through its own internal handler. This dedicated mapper with highest
 * priority intercepts those cases.</p>
 *
 * <p>References: DEF-V21-009</p>
 */
@Provider
@Priority(1) // highest priority to override RESTEasy built-in handler
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    private static final Logger LOG = Logger.getLogger(JsonProcessingExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(JsonProcessingException exception) {
        LOG.warnv("JSON deserialization error intercepted: {0}", exception.getOriginalMessage());

        String message = extractUserMessage(exception);
        String path = uriInfo != null
                ? uriInfo.getRequestUri().getPath()
                : "/api/v2";

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of("INVALID_JSON", message, List.of(), path))
                .build();
    }

    /**
     * Extracts a user-friendly message from the Jackson exception.
     * Strips the Jackson source-location suffix for cleaner output.
     */
    private String extractUserMessage(JsonProcessingException jpe) {
        String msg = jpe.getOriginalMessage();
        if (msg == null || msg.isBlank()) {
            msg = jpe.getMessage();
        }
        if (msg == null || msg.isBlank()) {
            return "Malformed JSON in request body";
        }
        int locationIdx = msg.indexOf("\n at [Source:");
        if (locationIdx > 0) {
            msg = msg.substring(0, locationIdx).trim();
        }
        return msg;
    }
}
