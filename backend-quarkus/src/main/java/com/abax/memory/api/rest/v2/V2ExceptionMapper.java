package com.abax.memory.api.rest.v2;

import com.abax.memory.api.dto.v2.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Exception mapper for API v2 endpoints.
 *
 * <p>Uses {@code @Priority(Priorities.USER - 100)} (4900) to take
 * precedence over the global v1 {@code ApiExceptionMapper} (5100).</p>
 *
 * <p>Maps domain and JAX-RS exceptions to the standardized
 * {@link ErrorResponse} JSON format defined in API Design §7.3.</p>
 *
 * <p>Handles Jackson deserialization errors (invalid enum values,
 * unrecognized JSON properties, malformed payloads) as 400 BAD_REQUEST
 * instead of 500.</p>
 *
 * <p>References: Architecture document §7.3, BR-009,
 * BUG-004, BUG-013, BUG-014</p>
 */
@Provider
@Priority(Priorities.USER - 100) // higher precedence than v1 mapper
public class V2ExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(V2ExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {

        // ── Unwrap RESTEasy UnhandledException ─────────────────────
        Throwable effective = exception;
        while (effective.getClass().getSimpleName().equals("UnhandledException")
                && effective.getCause() instanceof Exception cause) {
            effective = cause;
        }

        // ── Walk cause chain for Jackson errors ────────────────────
        // Jackson exceptions (InvalidFormatException for bad enums,
        // UnrecognizedPropertyException for unknown fields, etc.) may
        // be wrapped inside WebApplicationException (RESTEasy pattern)
        // or ProcessingException. Extract them from the cause chain.
        Throwable causeWalker = effective;
        while (causeWalker != null) {
            if (causeWalker instanceof JsonProcessingException jpe) {
                LOG.warnv("JSON processing error (v2): {0}", jpe.getOriginalMessage());
                return build(Response.Status.BAD_REQUEST, "INVALID_REQUEST",
                        extractJsonMessage(jpe), List.of());
            }
            causeWalker = causeWalker.getCause();
        }

        // ── NotFoundException (JAX-RS) → 404 ────────────────────────
        if (effective instanceof NotFoundException nfe) {
            LOG.infov("Not found (v2): {0}", nfe.getMessage());
            return build(Response.Status.NOT_FOUND, "NOT_FOUND",
                    nfe.getMessage() != null ? nfe.getMessage() : "Resource not found",
                    List.of());
        }

        // ── WebApplicationException — pass through its status ──────
        // RESTEasy wraps Jackson deserialization failures in
        // WebApplicationException(400) before they reach ExceptionMapper.
        // The cause-chain check above handles the Jackson details;
        // here we handle any other WebApplicationException.
        if (effective instanceof WebApplicationException wae) {
            Response waeResponse = wae.getResponse();
            if (waeResponse != null) {
                int status = waeResponse.getStatus();
                LOG.infov("WebApplicationException (v2): status={0}, message={1}",
                        status, wae.getMessage());
                String code = status >= 500 ? "INTERNAL_ERROR" : "INVALID_REQUEST";
                Response.Status statusEnum = Response.Status.fromStatusCode(status);
                String msg = wae.getMessage() != null ? wae.getMessage()
                        : (statusEnum != null ? statusEnum.getReasonPhrase() : "Error");
                return build(statusEnum != null ? statusEnum : Response.Status.BAD_REQUEST,
                        code, msg, List.of());
            }
        }

        // ── IllegalArgumentException → 400 ─────────────────────────
        if (effective instanceof IllegalArgumentException iae) {
            LOG.warnv("Invalid argument (v2): {0}", iae.getMessage());
            return build(Response.Status.BAD_REQUEST, "INVALID_REQUEST",
                    iae.getMessage() != null ? iae.getMessage() : "Invalid request",
                    List.of());
        }

        // ── ConstraintViolationException (Bean Validation) → 400 ───
        if (effective instanceof ConstraintViolationException cve) {
            LOG.warnv("Validation error (v2): {0}", cve.getMessage());
            List<String> details = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .toList();
            return build(Response.Status.BAD_REQUEST, "VALIDATION_ERROR",
                    "Validation failed", details);
        }

        // ── SecurityException → 403 ────────────────────────────────
        if (effective instanceof SecurityException se) {
            LOG.warnv("Security violation (v2): {0}", se.getMessage());
            return build(Response.Status.FORBIDDEN, "FORBIDDEN",
                    "Access denied", List.of());
        }

        // ── Generic / Unexpected → 500 ─────────────────────────────
        LOG.errorv(exception, "Unexpected server error (v2)");
        return build(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", List.of());
    }

    /**
     * Extracts a user-friendly message from a Jackson processing exception.
     * <p>For enum deserialization failures (e.g. {@code InvalidFormatException}),
     * the original message already contains a descriptive list of accepted values
     * because our {@code fromJson} factory methods in enums provide them.</p>
     */
    private String extractJsonMessage(JsonProcessingException jpe) {
        String msg = jpe.getOriginalMessage();
        if (msg == null || msg.isBlank()) {
            msg = jpe.getMessage();
        }
        if (msg == null || msg.isBlank()) {
            msg = "Malformed JSON in request body";
        }
        // Strip the Jackson location suffix for cleaner output
        int locationIdx = msg.indexOf("\n at [Source:");
        if (locationIdx > 0) {
            msg = msg.substring(0, locationIdx).trim();
        }
        return msg;
    }

    private Response build(Response.Status status, String errorCode,
                           String message, List<String> details) {
        String path = uriInfo != null
                ? uriInfo.getRequestUri().getPath()
                : "/api/v2";

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(errorCode, message, details, path))
                .build();
    }
}
