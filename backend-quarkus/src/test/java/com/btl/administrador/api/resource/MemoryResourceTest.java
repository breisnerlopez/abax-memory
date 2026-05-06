package com.btl.administrador.api.resource;

import com.btl.administrador.api.TestDataReset;
import com.btl.administrador.api.service.ProcessingWorkerService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class MemoryResourceTest {

    @Inject
    TestDataReset testDataReset;

    @Inject
    ProcessingWorkerService processingWorkerService;

    @BeforeEach
    void setUp() {
        testDataReset.reset();
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void createManualMemory_returnsCreatedContract() {
        String memoryId = given()
                .contentType("application/json")
                .body(memoryPayload("Runbook de red", "procedimiento", "MEDIA", List.of("infraestructura"), List.of("network"),
                        "## Pasos\n- Revisar enlace\n## Evidencias\n- Log de red", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("state", equalTo("APROBADA"))
                .body("processingStatus", equalTo("PENDING_INDEX"))
                .body("metadata.extractionStatus", equalTo("PARTIAL"))
                .extract()
                .path("id");

        given()
                .when()
                .get("/api/memorias/" + memoryId)
                .then()
                .statusCode(200)
                .body("id", equalTo(memoryId))
                .body("markdown", containsString(memoryId));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void createManual_invalidPayload_returnsApiErrorResponseWithCorrelationId() {
        given()
                .header("X-Correlation-Id", "corr-validation-001")
                .contentType("application/json")
                .body(memoryPayload(" ", "procedimiento", "MEDIA", List.of("infraestructura"), List.of("network"),
                        "# Diagnostico\nRevisar enlace", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(400)
                .header("X-Correlation-Id", equalTo("corr-validation-001"))
                .body("errorCode", equalTo("VALIDATION_ERROR"))
                .body("message", equalTo("Validation failed"))
                .body("correlationId", equalTo("corr-validation-001"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void createManual_invalidFrontmatter_returnsControlledError() {
        Map<String, Object> payload = memoryPayload("Runbook de red", "procedimiento", "MEDIA", List.of("infraestructura"), List.of("network"),
                "# Diagnostico\nRevisar enlace", Map.of("fuente", "manual"));
        ((Map<String, Object>) payload.get("frontmatter")).put("title", "Otro titulo");

        given()
                .contentType("application/json")
                .body(payload)
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_FRONTMATTER"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void createManual_invalidType_returnsControlledError() {
        given()
                .contentType("application/json")
                .body(memoryPayload("Runbook de red", "libre", "MEDIA", List.of("infraestructura"), List.of("network"),
                        "# Diagnostico\nRevisar enlace", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_MEMORY_TYPE"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void createFromMissingCase_returnsControlledError() {
        given()
                .contentType("application/json")
                .body(Map.of(
                        "caseId", "CASO-404",
                        "title", "Memoria desde caso",
                        "type", "incidente",
                        "criticality", "MEDIA",
                        "domains", List.of("operaciones"),
                        "metadata", Map.of("fuente", "caso"),
                        "frontmatter", frontmatter("Memoria desde caso", "incidente", "caso", "media", List.of("operaciones"), "caso")))
                .when()
                .post("/api/memorias/desde-caso")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("CASE_NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "reviewer-user", roles = {"memory-operator", "memory-reviewer"})
    void createCriticalMemory_thenApprove_returnsApprovedContract() {
        String memoryId = given()
                .contentType("application/json")
                .body(memoryPayload("Hallazgo critico QA", "incidente", "CRITICA", List.of("seguridad"), List.of("qa"),
                        "# Hallazgo\nRequiere aprobacion", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(202)
                .body("id", notNullValue())
                .body("state", equalTo("EN_REVISION"))
                .body("processingStatus", equalTo("PENDING_GIT"))
                .body("pullRequestRef", notNullValue())
                .extract()
                .path("id");

        given()
                .contentType("application/json")
                .body(Map.of("comentario", "Aprobada por QA"))
                .when()
                .post("/api/memorias/" + memoryId + "/aprobar")
                .then()
                .statusCode(200)
                .body("id", equalTo(memoryId))
                .body("state", equalTo("APROBADA"))
                .body("processingStatus", equalTo("PENDING_INDEX"))
                .body("commitSha", notNullValue());
    }

    @Test
    @TestSecurity(user = "reviewer-user", roles = {"memory-operator", "memory-reviewer", "memory-auditor"})
    void createCriticalMemory_thenObserve_keepsNonApprovedStateAndAuditTrace() {
        String memoryId = given()
                .contentType("application/json")
                .body(memoryPayload("Hallazgo critico QA", "incidente", "CRITICA", List.of("seguridad"), List.of("qa"),
                        "# Hallazgo\nRequiere aprobacion", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(202)
                .extract()
                .path("id");

        given()
                .contentType("application/json")
                .body(Map.of("decision", "OBSERVADA", "comentario", "Faltan evidencias"))
                .when()
                .post("/api/memorias/" + memoryId + "/revision")
                .then()
                .statusCode(200)
                .body("state", equalTo("OBSERVADA"));

        given()
                .when()
                .get("/api/memorias/" + memoryId + "/trazabilidad")
                .then()
                .statusCode(200)
                .body("events.action", hasItem("MEMORY_REVIEW_DECISION"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void approveCriticalMemory_withInsufficientRole_returnsForbiddenAndStateUnchanged() {
        String memoryId = given()
                .contentType("application/json")
                .body(memoryPayload("Hallazgo critico QA", "incidente", "CRITICA", List.of("seguridad"), List.of("qa"),
                        "# Hallazgo\nRequiere aprobacion", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(202)
                .body("state", equalTo("EN_REVISION"))
                .extract()
                .path("id");

        given()
                .contentType("application/json")
                .body(Map.of("comentario", "Intento no autorizado"))
                .when()
                .post("/api/memorias/" + memoryId + "/aprobar")
                .then()
                .statusCode(403);

        given()
                .when()
                .get("/api/memorias/" + memoryId)
                .then()
                .statusCode(200)
                .body("state", equalTo("EN_REVISION"));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"memory-operator", "memory-admin", "api-consumer"})
    void semanticSearch_excludesArchivedByDefault() {
        String memoryId = given()
                .contentType("application/json")
                .body(memoryPayload("Reset de contrasena", "procedimiento", "BAJA", List.of("soporte"), List.of("password"),
                        "# Accion\nRestablecer contrasena del usuario", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        processingWorkerService.processPendingJobs();

        given()
                .contentType("application/json")
                .body(Map.of("motivo", "Obsoleta"))
                .when()
                .post("/api/memorias/" + memoryId + "/archivar")
                .then()
                .statusCode(200)
                .body("state", equalTo("ARCHIVADA"));

        given()
                .contentType("application/json")
                .body(Map.of("consulta", "contrasena usuario", "topK", 10, "filtros", Map.of()))
                .when()
                .post("/api/busquedas/semantica")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"memory-operator", "memory-admin", "api-consumer"})
    void getArchivedMemoryById_returnsArchivedState() {
        String memoryId = given()
                .contentType("application/json")
                .body(memoryPayload("Runbook archivado consultable", "procedimiento", "BAJA", List.of("soporte"), List.of("archivo"),
                        "# Contenido\nMemoria archivada pero consultable", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .contentType("application/json")
                .body(Map.of("motivo", "Obsoleta"))
                .when()
                .post("/api/memorias/" + memoryId + "/archivar")
                .then()
                .statusCode(200)
                .body("state", equalTo("ARCHIVADA"));

        given()
                .when()
                .get("/api/memorias/" + memoryId)
                .then()
                .statusCode(200)
                .body("id", equalTo(memoryId))
                .body("state", equalTo("ARCHIVADA"))
                .body("metadata.fuente", equalTo("manual"));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"memory-operator", "memory-admin", "api-consumer"})
    void semanticSearch_semanticEquivalentQuery_returnsRelevantResult() {
        given()
                .contentType("application/json")
                .body(memoryPayload("Reset de credenciales", "procedimiento", "BAJA", List.of("soporte"), List.of("password"),
                        "# Accion\nRestablecer contrasena del usuario", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(201);

        processingWorkerService.processPendingJobs();

        given()
                .contentType("application/json")
                .body(Map.of("consulta", "reiniciar clave de acceso", "topK", 10, "filtros", Map.of()))
                .when()
                .post("/api/busquedas/semantica")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("score[0]", greaterThan(0.0f));
    }

    @Test
    @TestSecurity(user = "consumer-user", roles = {"memory-operator", "api-consumer"})
    void semanticSearch_returnsResultsOrderedByRelevance() {
        given()
                .contentType("application/json")
                .body(memoryPayload("Reset de credenciales completo", "procedimiento", "BAJA", List.of("soporte"), List.of("password"),
                        "# Accion\nRestablecer contrasena del usuario y recuperar acceso a la cuenta", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(201);

        given()
                .contentType("application/json")
                .body(memoryPayload("Guia de operador", "procedimiento", "BAJA", List.of("soporte"), List.of("usuario"),
                        "# Accion\nOrientacion para persona asignada", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(201);

        processingWorkerService.processPendingJobs();

        given()
                .contentType("application/json")
                .body(Map.of("consulta", "reiniciar clave de acceso usuario", "topK", 10, "filtros", Map.of()))
                .when()
                .post("/api/busquedas/semantica")
                .then()
                .statusCode(200)
                .body("", hasSize(2))
                .body("title", contains("Reset de credenciales completo", "Guia de operador"))
                .body("score[0]", greaterThan(0.0f));
    }

    @Test
    @TestSecurity(user = "consumer-user", roles = {"api-consumer"})
    void semanticSearch_withoutRelevantMatches_returnsEmptyResult() {
        given()
                .contentType("application/json")
                .body(Map.of("consulta", "concepto inexistente total", "topK", 10, "filtros", Map.of()))
                .when()
                .post("/api/busquedas/semantica")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void list_validFilters_returnsOnlyMatchingMemories() {
        given().contentType("application/json").body(memoryPayload("Runbook RRHH", "runbook", "BAJA", List.of("rrhh"), List.of("uno"),
                "# Uno", Map.of("fuente", "manual"))).when().post("/api/memorias").then().statusCode(201);
        given().contentType("application/json").body(memoryPayload("Incidente Seguridad", "incidente", "ALTA", List.of("seguridad"), List.of("dos"),
                "# Dos", Map.of("fuente", "manual"))).when().post("/api/memorias").then().statusCode(202);

        given()
                .when()
                .get("/api/memorias?type=runbook&origin=manual&domain=rrhh")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("title[0]", equalTo("Runbook RRHH"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void list_invalidStateFilter_returnsControlledError() {
        given()
                .header("X-Correlation-Id", "corr-filter-001")
                .when()
                .get("/api/memorias?state=desconocido")
                .then()
                .statusCode(400)
                .header("X-Correlation-Id", equalTo("corr-filter-001"))
                .body("errorCode", equalTo("INVALID_FILTER"))
                .body("correlationId", equalTo("corr-filter-001"));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"memory-operator", "memory-admin", "memory-auditor"})
    void createRelation_registersRelationAndAuditTrace() {
        String sourceId = given().contentType("application/json")
                .body(memoryPayload("Memoria origen", "procedimiento", "MEDIA", List.of("operaciones"), List.of("source"), "# Origen", Map.of("fuente", "manual")))
                .when().post("/api/memorias").then().statusCode(201).extract().path("id");

        String targetId = given().contentType("application/json")
                .body(memoryPayload("Memoria destino", "procedimiento", "MEDIA", List.of("operaciones"), List.of("target"), "# Destino", Map.of("fuente", "manual")))
                .when().post("/api/memorias").then().statusCode(201).extract().path("id");

        given()
                .contentType("application/json")
                .body(Map.of("targetMemoryId", targetId, "relationType", "RELACIONADA_CON"))
                .when()
                .post("/api/memorias/" + sourceId + "/relaciones")
                .then()
                .statusCode(201)
                .body("sourceMemoryId", equalTo(sourceId))
                .body("targetMemoryId", equalTo(targetId))
                .body("relationType", equalTo("RELACIONADA_CON"));

        given()
                .when()
                .get("/api/auditoria/memorias/" + sourceId)
                .then()
                .statusCode(200)
                .body("action", hasItem("RELATION_CREATED"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator", "memory-auditor"})
    void updateMemory_traceabilityShowsCreatorAndLastModifier() {
        String memoryId = given().contentType("application/json")
                .body(memoryPayload("Runbook base", "runbook", "BAJA", List.of("ops"), List.of("uno"), "## Pasos\n- Base", Map.of("fuente", "manual")))
                .when().post("/api/memorias").then().statusCode(201).extract().path("id");

        given()
                .contentType("application/json")
                .body(Map.of(
                        "title", "Runbook actualizado",
                        "type", "procedimiento",
                        "domains", List.of("ops"),
                        "tags", List.of("uno", "dos"),
                        "contenidoMarkdown", "## Pasos\n- Base\n## Decisiones\n- Ajuste\n## Evidencias\n- Commit\n## Resultados\n- Actualizado",
                        "metadata", Map.of("fuente", "manual", "editor", "usuario-b"),
                        "frontmatter", frontmatter("Runbook actualizado", "procedimiento", "manual", "baja", List.of("ops"), "manual")))
                .when()
                .patch("/api/memorias/" + memoryId)
                .then()
                .statusCode(200)
                .body("versionId", notNullValue())
                .body("metadata.extractionStatus", equalTo("COMPLETE"));

        given()
                .when()
                .get("/api/memorias/" + memoryId + "/trazabilidad")
                .then()
                .statusCode(200)
                .body("createdBy", equalTo("operator-user"))
                .body("lastModifiedBy", equalTo("operator-user"))
                .body("events.action", hasItem("MEMORY_UPDATED"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator", "memory-auditor"})
    void createMemory_traceabilityExposesOriginVersionAndEvents() {
        String memoryId = given().contentType("application/json")
                .body(memoryPayload("Runbook auditado", "runbook", "BAJA", List.of("ops"), List.of("uno"), "## Pasos\n- Base", Map.of("fuente", "manual")))
                .when().post("/api/memorias").then().statusCode(201).extract().path("id");

        given()
                .when()
                .get("/api/memorias/" + memoryId + "/trazabilidad")
                .then()
                .statusCode(200)
                .body("memoryId", equalTo(memoryId))
                .body("origin", equalTo("MANUAL"))
                .body("versionId", notNullValue())
                .body("commitSha", notNullValue())
                .body("events", hasSize(1));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void createManual_withForcedGitFailure_returnsControlledError() {
        given()
                .contentType("application/json")
                .body(memoryPayload("Runbook fallido", "procedimiento", "BAJA", List.of("infraestructura"), List.of("network"),
                        "# Diagnostico\nRevisar enlace", Map.of("fuente", "manual", "forceGitFailure", "true")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(503)
                .body("errorCode", equalTo("GIT_PERSISTENCE_FAILED"));
    }

    @Test
    @TestSecurity(user = "consumer-user", roles = {"api-consumer"})
    void semanticSearch_invalidTopK_returnsControlledError() {
        given()
                .contentType("application/json")
                .body(Map.of("consulta", "contrasena usuario", "topK", 99, "filtros", Map.of()))
                .when()
                .post("/api/busquedas/semantica")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("INVALID_TOPK"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void archiveMemory_withWrongRole_returnsForbidden() {
        String memoryId = given()
                .contentType("application/json")
                .body(memoryPayload("Runbook archivado", "procedimiento", "MEDIA", List.of("infraestructura"), List.of("network"),
                        "# Diagnostico\nRevisar enlace", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .contentType("application/json")
                .body(Map.of("motivo", "No autorizado"))
                .when()
                .post("/api/memorias/" + memoryId + "/archivar")
                .then()
                .statusCode(403);
    }

    @Test
    void createManualMemory_withoutToken_returnsUnauthorized() {
        given()
                .contentType("application/json")
                .body(memoryPayload("Runbook de red", "procedimiento", "MEDIA", List.of("infraestructura"), List.of("network"),
                        "# Diagnostico\nRevisar enlace", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "auditor-user", roles = {"memory-auditor"})
    void createManualMemory_withWrongRole_returnsForbidden() {
        given()
                .contentType("application/json")
                .body(memoryPayload("Runbook de red", "procedimiento", "MEDIA", List.of("infraestructura"), List.of("network"),
                        "# Diagnostico\nRevisar enlace", Map.of("fuente", "manual")))
                .when()
                .post("/api/memorias")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void getMissingMemory_returnsControlledError() {
        given()
                .when()
                .get("/api/memorias/MEM-404")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("MEMORY_NOT_FOUND"));
    }

    // ISSUE #9: api-consumer role filtering

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void list_operatorRole_seesAllNonExcludedMemories() {
        given().contentType("application/json")
                .body(memoryPayload("Runbook aprobado", "runbook", "BAJA", List.of("ops"), List.of("a"), "# A", Map.of("fuente", "manual")))
                .when().post("/api/memorias").then().statusCode(201);
        given().contentType("application/json")
                .body(memoryPayload("Incidente en revision", "incidente", "CRITICA", List.of("seguridad"), List.of("b"), "# B", Map.of("fuente", "manual")))
                .when().post("/api/memorias").then().statusCode(202);

        given()
                .when()
                .get("/api/memorias")
                .then()
                .statusCode(200)
                .body("", hasSize(2))
                .body("state", contains("APROBADA", "EN_REVISION"));
    }

    @Test
    @TestSecurity(user = "consumer-user", roles = {"api-consumer"})
    void list_apiConsumerOnly_afterResetReturnsEmpty() {
        // After reset there are no APROBADA memories; api-consumer sees empty list.
        // The role filter prevents leaking non-APROBADA memories.
        given()
                .when()
                .get("/api/memorias")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    @TestSecurity(user = "consumer-user", roles = {"api-consumer"})
    void getById_apiConsumerOnly_missingMemory_returnsNotFound() {
        // api-consumer trying to access a non-existent memory should get 404,
        // not leak that a memory exists but is inaccessible.
        given()
                .when()
                .get("/api/memorias/MEM-404")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("MEMORY_NOT_FOUND"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> memoryPayload(String title, String type, String criticality, List<String> domains, List<String> tags,
                                              String markdown, Map<String, String> metadata) {
        return Map.of(
                "title", title,
                "type", type,
                "criticality", criticality,
                "domains", domains,
                "tags", tags,
                "contenidoMarkdown", markdown,
                "metadata", metadata,
                "frontmatter", frontmatter(title, type, "manual", criticality.toLowerCase(), domains, metadata.get("fuente")));
    }

    private Map<String, Object> frontmatter(String title, String type, String origin, String criticality, List<String> domains, String fuente) {
        return new java.util.LinkedHashMap<>(Map.of(
                "title", title,
                "type", type,
                "origin", origin,
                "criticality", criticality,
                "domains", domains,
                "metadata", Map.of("fuente", fuente)));
    }
}
