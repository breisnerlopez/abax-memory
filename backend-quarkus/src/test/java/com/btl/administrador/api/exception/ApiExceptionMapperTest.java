package com.btl.administrador.api.exception;

import com.btl.administrador.api.dto.ApiErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonLocation;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ApiExceptionMapperTest {

    @Inject
    ApiExceptionMapper mapper;

    @Inject
    CorrelationIdHolder correlationIdHolder;

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
}
