package com.btl.administrador.api.resource;

import com.btl.administrador.api.dto.AuditEventResponse;
import com.btl.administrador.api.security.MemoryRoles;
import com.btl.administrador.api.service.AuditService;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

@Path("/api/auditoria")
@Produces(MediaType.APPLICATION_JSON)
public class AuditResource {

    @Inject
    AuditService auditService;

    @GET
    @Path("/memorias/{id}")
    @Operation(summary = "Consultar trazabilidad de una memoria")
    @RolesAllowed({MemoryRoles.MEMORY_AUDITOR, MemoryRoles.MEMORY_ADMIN})
    public List<AuditEventResponse> findByMemoryId(@PathParam("id") String id) {
        return auditService.findByEntityId(id);
    }
}
