package com.abax.memory.api.rest.v2;

import com.abax.memory.api.dto.v2.ErrorResponse;
import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotFoundException;
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
 * Exception mapper for API v2 endpoints.
 *
 * <p>Uses {@code @Priority(Priorities.USER)} (5000) to take precedence
 * over the global v1 {@code ApiExceptionMapper} (which defaults to
 * {@code Priorities.USER + 100} = 5100).</p>
 *
 * <p>Maps domain and JAX-RS exceptions to the standardized
 * {@link ErrorResponse} JSON format defined in API Design §7.3.</p>
 *
 * <p>References: Architecture document §7.3, BR-009</p>
 */
@Provider
@Priority(Priorities.USER - 100) // higher precedence than v1 mapper
public class V2ExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(V2ExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {

        // ── NotFoundException (JAX-RS) → 404 ────────────────────────
        if (exception instanceof NotFoundException nfe) {
            LOG.infov("Not found (v2): {0}", nfe.getMessage());
            return build(Response.Status.NOT_FOUND, "NOT_FOUND",
                    nfe.getMessage() != null ? nfe.getMessage() : "Resource not found",
                    List.of());
        }

        // ── IllegalArgumentException → 400 ─────────────────────────
        if (exception instanceof IllegalArgumentException iae) {
            LOG.warnv("Invalid argument (v2): {0}", iae.getMessage());
            return build(Response.Status.BAD_REQUEST, "INVALID_REQUEST",
                    iae.getMessage() != null ? iae.getMessage() : "Invalid request",
                    List.of());
        }

        // ── ConstraintViolationException (Bean Validation) → 400 ───
        if (exception instanceof ConstraintViolationException cve) {
            LOG.warnv("Validation error (v2): {0}", cve.getMessage());
            List<String> details = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .toList();
            return build(Response.Status.BAD_REQUEST, "VALIDATION_ERROR",
                    "Validation failed", details);
        }

        // ── SecurityException → 403 ────────────────────────────────
        if (exception instanceof SecurityException se) {
            LOG.warnv("Security violation (v2): {0}", se.getMessage());
            return build(Response.Status.FORBIDDEN, "FORBIDDEN",
                    "Access denied", List.of());
        }

        // ── Generic / Unexpected → 500 ─────────────────────────────
        LOG.errorv(exception, "Unexpected server error (v2)");
        return build(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", List.of());
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
