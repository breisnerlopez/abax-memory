package com.btl.administrador.api.resource;

import com.btl.administrador.api.dto.CaseResponse;
import com.btl.administrador.api.dto.CloseCaseRequest;
import com.btl.administrador.api.dto.CreateCaseRequest;
import com.btl.administrador.api.security.MemoryRoles;
import com.btl.administrador.api.service.CaseService;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/api/casos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CaseResource {

    @Inject
    CaseService caseService;

    @POST
    @Operation(summary = "Crear un caso operativo")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN})
    public Response create(@Valid CreateCaseRequest request) {
        return Response.status(Response.Status.CREATED).entity(caseService.create(request)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar un caso por identificador")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN, MemoryRoles.MEMORY_AUDITOR, MemoryRoles.API_CONSUMER})
    public CaseResponse getById(@PathParam("id") String id) {
        return caseService.getById(id);
    }

    @POST
    @Path("/{id}/cerrar")
    @Operation(summary = "Cerrar un caso operativo")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN})
    public CaseResponse close(@PathParam("id") String id, @Valid CloseCaseRequest request) {
        return caseService.close(id, request);
    }
}
