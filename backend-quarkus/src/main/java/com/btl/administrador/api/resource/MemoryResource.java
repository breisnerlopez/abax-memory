package com.btl.administrador.api.resource;

import com.btl.administrador.api.dto.ApproveMemoryRequest;
import com.btl.administrador.api.dto.ArchiveMemoryRequest;
import com.btl.administrador.api.dto.CreateMemoryFromCaseRequest;
import com.btl.administrador.api.dto.CreateMemoryRequest;
import com.btl.administrador.api.dto.CreateRelationRequest;
import com.btl.administrador.api.dto.MemoryResponse;
import com.btl.administrador.api.dto.MemoryTraceabilityResponse;
import com.btl.administrador.api.dto.RelationResponse;
import com.btl.administrador.api.dto.ReviewMemoryRequest;
import com.btl.administrador.api.dto.SearchMemoryRequest;
import com.btl.administrador.api.dto.SearchResultResponse;
import com.btl.administrador.api.dto.UpdateMemoryRequest;
import com.btl.administrador.api.security.MemoryRoles;
import com.btl.administrador.api.service.MemoryService;
import com.btl.administrador.api.service.SearchService;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

@Path("/api/memorias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MemoryResource {

    @Inject
    MemoryService memoryService;

    @Inject
    SearchService searchService;

    @POST
    @Operation(summary = "Crear una memoria manual")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN})
    public Response createManual(@Valid CreateMemoryRequest request) {
        MemoryResponse response = memoryService.createManual(request);
        Response.Status status = response.pullRequestRef() == null ? Response.Status.CREATED : Response.Status.ACCEPTED;
        return Response.status(status).entity(response).build();
    }

    @POST
    @Path("/desde-caso")
    @Operation(summary = "Crear una memoria desde un caso existente")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN})
    public Response createFromCase(@Valid CreateMemoryFromCaseRequest request) {
        MemoryResponse response = memoryService.createFromCase(request);
        Response.Status status = response.pullRequestRef() == null ? Response.Status.CREATED : Response.Status.ACCEPTED;
        return Response.status(status).entity(response).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar detalle de memoria")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN, MemoryRoles.MEMORY_AUDITOR, MemoryRoles.API_CONSUMER})
    public MemoryResponse getById(@PathParam("id") String id) {
        return memoryService.getById(id);
    }

    @GET
    @Path("/{id}/trazabilidad")
    @Operation(summary = "Consultar trazabilidad completa de una memoria")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN, MemoryRoles.MEMORY_AUDITOR, MemoryRoles.API_CONSUMER})
    public MemoryTraceabilityResponse traceability(@PathParam("id") String id) {
        return memoryService.traceability(id);
    }

    @GET
    @Operation(summary = "Listar memorias con filtros basicos")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN, MemoryRoles.MEMORY_AUDITOR, MemoryRoles.API_CONSUMER})
    public List<MemoryResponse> list(@QueryParam("type") String type,
                                     @QueryParam("state") String state,
                                     @QueryParam("origin") String origin,
                                     @QueryParam("domain") String domain,
                                     @QueryParam("includeArchived") @DefaultValue("false") boolean includeArchived) {
        return memoryService.list(type, state, origin, domain, includeArchived);
    }

    @POST
    @Path("/{id}/aprobar")
    @Operation(summary = "Aprobar una memoria critica y disparar indexacion")
    @RolesAllowed({MemoryRoles.MEMORY_REVIEWER, MemoryRoles.MEMORY_ADMIN})
    public MemoryResponse approve(@PathParam("id") String id, @Valid ApproveMemoryRequest request) {
        return memoryService.approve(id, request);
    }

    @POST
    @Path("/{id}/revision")
    @Operation(summary = "Registrar observacion o rechazo de memoria en revision")
    @RolesAllowed({MemoryRoles.MEMORY_REVIEWER, MemoryRoles.MEMORY_ADMIN})
    public MemoryResponse review(@PathParam("id") String id, @Valid ReviewMemoryRequest request) {
        return memoryService.review(id, request);
    }

    @POST
    @Path("/{id}/archivar")
    @Operation(summary = "Archivar una memoria")
    @RolesAllowed(MemoryRoles.MEMORY_ADMIN)
    public MemoryResponse archive(@PathParam("id") String id, @Valid ArchiveMemoryRequest request) {
        return memoryService.archive(id, request);
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Actualizar contenido o metadata de una memoria")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN})
    public MemoryResponse update(@PathParam("id") String id, @Valid UpdateMemoryRequest request) {
        return memoryService.update(id, request);
    }

    @POST
    @Path("/search")
    @Operation(summary = "Busqueda semantica de memorias con embeddings reales (OpenAI + Qdrant)")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN, MemoryRoles.MEMORY_AUDITOR, MemoryRoles.API_CONSUMER})
    public List<SearchResultResponse> search(@Valid SearchMemoryRequest request) {
        return searchService.search(request);
    }

    @POST
    @Path("/{id}/relaciones")
    @Operation(summary = "Registrar una relacion basica de trazabilidad")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN})
    public Response createRelation(@PathParam("id") String id, @Valid CreateRelationRequest request) {
        RelationResponse response = memoryService.createRelation(id, request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
