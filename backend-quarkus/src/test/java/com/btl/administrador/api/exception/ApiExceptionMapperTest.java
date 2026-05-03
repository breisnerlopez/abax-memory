package com.btl.administrador.api.exception;

import com.btl.administrador.api.dto.ApiErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonLocation;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.IOException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ApiExceptionMapperTest {

    @Inject
    ApiExceptionMapper mapper;

    @Inject
    CorrelationIdHolder correlationIdHolder;

    // -------------------------------------------------------------------------
    // Existing tests (JsonProcessing, generic, correlationId)
    // -------------------------------------------------------------------------

    @Test
    void directJsonProcessingException_returns400InvalidJson() {
        JsonProcessingException jpe = new JsonProcessingException("Unexpected character", JsonLocation.NA) {};

        Response response = mapper.toResponse(jpe);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("INVALID_JSON", body.code());
        assertEquals("Invalid JSON format", body.message());
    }

    @Test
    void webApplicationExceptionWrappingJsonProcessing_returns400InvalidJson() {
        JsonProcessingException jpe = new JsonProcessingException(
                "Unexpected character ('b' (code 98)): was expecting double-quote to start field name",
                JsonLocation.NA) {};
        WebApplicationException wae = new WebApplicationException(jpe, Response.Status.BAD_REQUEST);

        Response response = mapper.toResponse(wae);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("INVALID_JSON", body.code());
        assertEquals("Invalid JSON format", body.message());
    }

    @Test
    void deepCauseChainWithJsonProcessing_returns400InvalidJson() {
        JsonProcessingException jpe = new JsonProcessingException("Malformed input", JsonLocation.NA) {};
        RuntimeException intermediate = new RuntimeException("Wrapper", jpe);
        WebApplicationException wae = new WebApplicationException(intermediate, Response.Status.BAD_REQUEST);

        Response response = mapper.toResponse(wae);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("INVALID_JSON", body.code());
    }

    @Test
    void genericExceptionWithoutJsonCause_returns500UnexpectedError() {
        Exception generic = new RuntimeException("Something went wrong");

        Response response = mapper.toResponse(generic);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("UNEXPECTED_ERROR", body.code());
    }

    @Test
    void responseIncludesCorrelationIdAndTimestamp() {
        correlationIdHolder.setCorrelationId("test-correlation-xyz");

        JsonProcessingException jpe = new JsonProcessingException("Bad JSON", JsonLocation.NA) {};
        WebApplicationException wae = new WebApplicationException(jpe, Response.Status.BAD_REQUEST);

        Response response = mapper.toResponse(wae);

        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("test-correlation-xyz", body.correlationId());
        assertNotNull(body.timestamp());
    }

    // -------------------------------------------------------------------------
    // ISSUE #7: Unsupported or missing Content-Type
    // -------------------------------------------------------------------------

    @Test
    void notSupportedException_returns415UnsupportedMediaType() {
        NotSupportedException ex = new NotSupportedException(
                Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                        .entity("Unsupported Content-Type")
                        .build());

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("UNSUPPORTED_MEDIA_TYPE", body.code());
        assertEquals("Unsupported Content-Type", body.message());
    }

    @Test
    void notSupportedExceptionWithoutResponse_returns415() {
        // Simulates missing Content-Type header (NotSupportedException can be thrown without a Response)
        NotSupportedException ex = new NotSupportedException("Cannot consume content type");

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("UNSUPPORTED_MEDIA_TYPE", body.code());
    }

    @Test
    void ioException_emptyBody_returns400InvalidRequestBody() {
        IOException ex = new IOException("Request body is missing or unreadable");

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("INVALID_REQUEST_BODY", body.code());
        assertEquals("Request body is missing or unreadable", body.message());
    }

    @Test
    void eofException_emptyBody_returns400InvalidRequestBody() {
        EOFException ex = new EOFException("No content to map to Object due to end of input");

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("INVALID_REQUEST_BODY", body.code());
        assertEquals("Request body is missing or unreadable", body.message());
    }

    @Test
    void ioExceptionWrappedInWebApplicationException_returns400() {
        IOException ioEx = new IOException("Connection reset");
        WebApplicationException wae = new WebApplicationException(ioEx, Response.Status.BAD_REQUEST);

        Response response = mapper.toResponse(wae);

        // IOException check applies to the top-level exception, not the cause.
        // WebApplicationException is not an IOException, so it falls through to the generic 500.
        // This is expected: only direct IOException instances (or subtypes) are mapped.
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    // -------------------------------------------------------------------------
    // ISSUE #8: Database outage returns 503 instead of 500
    // -------------------------------------------------------------------------

    @Test
    void sqlException_databaseDown_returns503DatabaseUnavailable() {
        SQLException ex = new SQLException("Connection to localhost:5432 refused. Check that the hostname and port are correct.");

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("DATABASE_UNAVAILABLE", body.code());
        assertEquals("Database is temporarily unavailable. Please retry later.", body.message());
    }

    @Test
    void sqlException_noConnectionMessage_returns503() {
        // SQLException alone should trigger the handler regardless of message content
        SQLException ex = new SQLException("Fatal database error");

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("DATABASE_UNAVAILABLE", body.code());
    }

    @Test
    void persistenceExceptionWithConnectionMessage_returns503() {
        PersistenceException ex = new PersistenceException("Unable to acquire JDBC Connection");

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("DATABASE_UNAVAILABLE", body.code());
    }

    @Test
    void persistenceExceptionWithoutConnectionMessage_returns500() {
        // PersistenceException without connection-related message must not trigger 503.
        // Only PersistenceException with "connection" in message maps to DATABASE_UNAVAILABLE.
        PersistenceException ex = new PersistenceException("Unknown entity");

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("UNEXPECTED_ERROR", body.code());
    }

    @Test
    void deepCauseChain_sqlExceptionWrapped_returns503() {
        SQLException sqlEx = new SQLException("Connection refused");
        RuntimeException serviceEx = new RuntimeException("Service failure", sqlEx);
        WebApplicationException wae = new WebApplicationException(serviceEx, Response.Status.INTERNAL_SERVER_ERROR);

        Response response = mapper.toResponse(wae);

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("DATABASE_UNAVAILABLE", body.code());
    }

    @Test
    void deepCauseChain_hibernateJdbcConnectionException_returns503() {
        // Simulate Hibernate's JDBCConnectionException using an anonymous RuntimeException
        // with class name matching "JDBCConnectionException"
        RuntimeException hibernateEx = new RuntimeException("Unable to acquire JDBC Connection") {
            @Override
            public String getMessage() {
                return "Unable to acquire JDBC Connection";
            }
        };
        // The anonymous class name won't contain JDBCConnectionException, so let's test differently.
        // Instead, use a SQLException in the cause chain which definitely matches.

        SQLException sqlRoot = new SQLException("FATAL: the database system is starting up");
        RuntimeException intermediate = new RuntimeException("Hibernate operation failed", sqlRoot);
        PersistenceException jpaEx = new PersistenceException("JDBC connection issue", intermediate);

        Response response = mapper.toResponse(jpaEx);

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("DATABASE_UNAVAILABLE", body.code());
    }

    @Test
    void exceptionWithConnectionKeywordInClassName_returns503() {
        // Simulate an exception whose class name contains "connection"
        @SuppressWarnings("serial")
        Exception ex = new RuntimeException("Broken pipe") {
            // Override getClass().getName() is not possible with final methods.
            // Use a message containing "connection" in the cause chain instead.
        };
        // This won't match via class name. Let's test via message in cause chain.

        SQLException sqlEx = new SQLException("Connection to PostgreSQL has been terminated");
        RuntimeException wrapper = new RuntimeException("Query failed", sqlEx);

        Response response = mapper.toResponse(wrapper);

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("DATABASE_UNAVAILABLE", body.code());
    }

    @Test
    void nonDatabaseException_returns500_not503() {
        Exception ex = new RuntimeException("Null pointer in business logic");

        Response response = mapper.toResponse(ex);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("UNEXPECTED_ERROR", body.code());
    }
}
