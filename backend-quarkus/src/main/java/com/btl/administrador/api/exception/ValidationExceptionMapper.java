package com.btl.administrador.api.exception;

import com.btl.administrador.api.dto.ApiErrorResponse;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.util.List;

@Provider
@Priority(Priorities.USER)
public class ValidationExceptionMapper implements ExceptionMapper<ResteasyReactiveViolationException> {

    @Inject
    CorrelationIdHolder correlationIdHolder;

    @Override
    public Response toResponse(ResteasyReactiveViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .toList();

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiErrorResponse(
                        "VALIDATION_ERROR",
                        "Validation failed",
                        correlationIdHolder.getCorrelationId(),
                        details,
                        OffsetDateTime.now()))
                .build();
    }
}
