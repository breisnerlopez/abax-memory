package com.btl.administrador.api.exception;

import com.btl.administrador.api.dto.ApiErrorResponse;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.List;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class);

    @Inject
    CorrelationIdHolder correlationIdHolder;

    @Override
    public Response toResponse(Exception exception) {
        // Security exceptions must be handled before the catch-all to preserve proper HTTP status codes.
        // Without these handlers, security exceptions were incorrectly mapped to 500 (BUG-QA-REAL-001).
        if (exception instanceof AuthenticationFailedException authEx) {
            LOG.warnv("Authentication failed: {0}", authEx.getMessage());
            return build(Response.Status.UNAUTHORIZED.getStatusCode(), "UNAUTHORIZED",
                    "Authentication required", List.of());
        }

        if (exception instanceof UnauthorizedException unauthEx) {
            LOG.warnv("Unauthorized access: {0}", unauthEx.getMessage());
            return build(Response.Status.UNAUTHORIZED.getStatusCode(), "UNAUTHORIZED",
                    "Authentication required", List.of());
        }

        if (exception instanceof NotAuthorizedException notAuthEx) {
            LOG.warnv("Not authorized: {0}", notAuthEx.getMessage());
            return build(Response.Status.UNAUTHORIZED.getStatusCode(), "UNAUTHORIZED",
                    "Authentication required", List.of());
        }

        if (exception instanceof ForbiddenException forbiddenEx) {
            LOG.warnv("Forbidden access: {0}", forbiddenEx.getMessage());
            return build(Response.Status.FORBIDDEN.getStatusCode(), "FORBIDDEN",
                    "Access denied", List.of());
        }

        if (exception instanceof jakarta.ws.rs.ForbiddenException forbiddenEx) {
            LOG.warnv("Forbidden access (JAX-RS): {0}", forbiddenEx.getMessage());
            return build(Response.Status.FORBIDDEN.getStatusCode(), "FORBIDDEN",
                    "Access denied", List.of());
        }

        if (exception instanceof NotAllowedException notAllowedEx) {
            LOG.warnv("Method not allowed: {0}", notAllowedEx.getMessage());
            return build(Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), "METHOD_NOT_ALLOWED",
                    "HTTP method not allowed for this endpoint", List.of());
        }

        if (exception instanceof ApiException apiException) {
            return build(apiException.getStatus(), apiException.getCode(), apiException.getMessage(), List.of());
        }

        if (exception instanceof ConstraintViolationException constraintViolationException) {
            List<String> details = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .toList();
            return build(Response.Status.BAD_REQUEST.getStatusCode(), "VALIDATION_ERROR", "Validation failed", details);
        }

        LOG.errorv(exception, "Unexpected server error");
        return build(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "UNEXPECTED_ERROR", "Unexpected server error", List.of());
    }

    private Response build(int status, String code, String message, List<String> details) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiErrorResponse(code, message, correlationIdHolder.getCorrelationId(), details, OffsetDateTime.now()))
                .build();
    }
}
