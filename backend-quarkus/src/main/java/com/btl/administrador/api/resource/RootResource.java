package com.btl.administrador.api.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class RootResource {

    @GET
    public Map<String, String> root() {
        return Map.of(
                "service", "abax-memory",
                "version", "1.0.0",
                "status", "UP");
    }
}
