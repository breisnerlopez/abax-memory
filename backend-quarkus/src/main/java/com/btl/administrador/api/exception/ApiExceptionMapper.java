package com.btl.administrador.api.exception;

import com.btl.administrador.api.dto.ApiErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Provider
@Priority(Priorities.USER) // lower priority than v2 mapper (v2 wins for API v2 paths)
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

        // ISSUE #7: Unsupported Content-Type (e.g., missing, text/plain, x-www-form-urlencoded)
        if (exception instanceof NotSupportedException notSupportedEx) {
            LOG.warnv("Unsupported media type: {0}", notSupportedEx.getMessage());
            return build(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), "UNSUPPORTED_MEDIA_TYPE",
                    "Unsupported Content-Type", List.of());
        }

        // JSON processing errors must be checked BEFORE IOException because
        // JsonProcessingException extends IOException (Jackson 2.x hierarchy).
        if (isJsonProcessingError(exception)) {
            LOG.warnv("Invalid JSON format: {0}", exception.getMessage());
            return build(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_JSON",
                    "Invalid JSON format", List.of());
        }

        // ISSUE #7: Empty or unreadable request body
        if (exception instanceof IOException ioException) {
            LOG.warnv("Request body read error: {0}", ioException.getMessage());
            return build(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_REQUEST_BODY",
                    "Request body is missing or unreadable", List.of());
        }

        // ISSUE #8: Database connection failures must return 503 instead of 500
        if (isDatabaseConnectionError(exception)) {
            LOG.errorv(exception, "Database connection failure detected");
            return build(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), "DATABASE_UNAVAILABLE",
                    "Database is temporarily unavailable. Please retry later.", List.of());
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

        // ISSUE #7: NullPointerException caused by null request body (empty body with valid Content-Type)
        // When JAX-RS sets the request parameter to null because the body is empty,
        // any access to the parameter's fields triggers NPE. Map to 400.
        if (exception instanceof NullPointerException npe) {
            if (isNullRequestBody(npe)) {
                LOG.warnv("Null request body detected: {0}", npe.getMessage());
                return build(Response.Status.BAD_REQUEST.getStatusCode(), "INVALID_REQUEST_BODY",
                        "Request body is required", List.of());
            }
            // For other NullPointerExceptions, fall through to 500
        }

        LOG.errorv(exception, "Unexpected server error");
        return build(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "UNEXPECTED_ERROR", "Unexpected server error", List.of());
    }

    /**
     * Traverses the exception cause chain to detect JSON processing errors.
     * Quarkus wraps {@code JsonParseException} into {@code WebApplicationException}
     * before it reaches any {@code ExceptionMapper}, so a simple {@code instanceof}
     * check on the top-level exception is insufficient.
     */
    private boolean isJsonProcessingError(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof JsonProcessingException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Traverses the exception cause chain to detect database connection failures.
     * PostgreSQL outages, Hibernate JDBC connection errors, and JPA persistence
     * exceptions with connection-related causes are all mapped to HTTP 503.
     *
     * <p>Detection criteria (checked at every level of the cause chain):
     * <ul>
     *   <li>Instance of {@link java.sql.SQLException} (covers {@code org.postgresql.util.PSQLException})</li>
     *   <li>Class name contains {@code JDBCConnectionException} (Hibernate)</li>
     *   <li>{@link jakarta.persistence.PersistenceException} whose message mentions "connection"</li>
     *   <li>Class name indicates a database driver/package: {@code org.postgresql}, {@code jdbc},
     *       or {@code sql} (excluding JSON-related classes)</li>
     *   <li>Message contains database-specific keywords: {@code jdbc}, {@code org.postgresql},
     *       or {@code sqlstate}</li>
     * </ul>
     *
     * <p>Generic "connection" in non-database exception messages (e.g.,
     * {@code IOException("Connection reset")}) is intentionally excluded
     * to avoid false positives.
     */
    private boolean isDatabaseConnectionError(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            // Direct SQLException instance (covers org.postgresql.util.PSQLException)
            if (current instanceof SQLException) {
                return true;
            }

            String className = current.getClass().getName();
            String lowerClassName = className.toLowerCase();

            // Hibernate wraps JDBC connection errors in JDBCConnectionException
            if (className.contains("JDBCConnectionException")) {
                return true;
            }

            // JPA PersistenceException with connection-related message
            if (current instanceof PersistenceException) {
                String msg = current.getMessage();
                if (msg != null && msg.toLowerCase().contains("connection")) {
                    return true;
                }
            }

            // Database driver or database-related class by name
            // (exclude JSON-related classes to avoid matching "json" substring in "sql")
            boolean isDatabaseClass = lowerClassName.contains("org.postgresql")
                    || lowerClassName.contains("jdbc")
                    || (lowerClassName.contains("sql") && !lowerClassName.contains("json"));

            if (isDatabaseClass) {
                return true;
            }

            // Message-based detection: only database-specific keywords
            String message = current.getMessage();
            if (message != null) {
                String lowerMsg = message.toLowerCase();
                if (lowerMsg.contains("jdbc")
                        || lowerMsg.contains("org.postgresql")
                        || lowerMsg.contains("sqlstate")) {
                    return true;
                }
                // "connection" in message only counts when the class is database-related
                if (lowerMsg.contains("connection") && isDatabaseClass) {
                    return true;
                }
            }

            current = current.getCause();
        }
        return false;
    }

    /**
     * Detects NullPointerException caused by a null request body.
     * When an empty body is sent with a valid Content-Type, JAX-RS/RESTEasy
     * may deserialize it as {@code null} instead of throwing an exception.
     * Subsequent field access on the null parameter triggers NPE.
     *
     * <p>Detection criteria:
     * <ul>
     *   <li>Message contains {@code "request" is null} (JVM default NPE message pattern)</li>
     *   <li>Stack trace contains the resource/endpoint method that receives the body parameter</li>
     * </ul>
     */
    private boolean isNullRequestBody(NullPointerException npe) {
        String message = npe.getMessage();
        if (message == null) {
            return false;
        }
        // JVM produces messages like: Cannot invoke "...method()" because "request" is null
        if (message.contains("\"request\" is null") || message.contains("'request' is null")) {
            return true;
        }
        // Also check the stack trace for the resource method
        for (StackTraceElement ste : npe.getStackTrace()) {
            String className = ste.getClassName();
            if (className.contains(".resource.") || className.contains(".api.resource.")) {
                return true;
            }
        }
        return false;
    }

    private Response build(int status, String code, String message, List<String> details) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiErrorResponse(code, message, correlationIdHolder.getCorrelationId(), details, OffsetDateTime.now()))
                .build();
    }
}
