package com.btl.administrador.api.resource;

import com.btl.administrador.api.dto.SearchMemoryRequest;
import com.btl.administrador.api.dto.SearchResultResponse;
import com.btl.administrador.api.security.MemoryRoles;
import com.btl.administrador.api.service.SearchService;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

@Path("/api/busquedas/semantica")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SearchResource {

    @Inject
    SearchService searchService;

    @POST
    @Operation(summary = "Ejecutar busqueda semantica con filtros estructurados")
    @RolesAllowed({MemoryRoles.MEMORY_OPERATOR, MemoryRoles.MEMORY_ADMIN, MemoryRoles.MEMORY_AUDITOR, MemoryRoles.API_CONSUMER})
    public List<SearchResultResponse> search(@Valid SearchMemoryRequest request) {
        return searchService.search(request);
    }
}
