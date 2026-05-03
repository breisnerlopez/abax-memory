package com.btl.administrador.api.resource;

import com.btl.administrador.api.TestDataReset;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class CaseResourceTest {

    @Inject
    TestDataReset testDataReset;

    @BeforeEach
    void setUp() {
        testDataReset.reset();
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator", "memory-auditor"})
    void createAndGetCase_usesSpanishContractPath() {
        String caseId = given()
                .contentType("application/json")
                .body(validCasePayload())
                .when()
                .post("/api/casos")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("ABIERTO"))
                .extract()
                .path("id");

        given()
                .when()
                .get("/api/casos/" + caseId)
                .then()
                .statusCode(200)
                .body("id", equalTo(caseId))
                .body("domain", equalTo("regularizacion"))
                .body("status", equalTo("ABIERTO"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void createCase_missingField_returnsConsistentValidationError() {
        given()
                .contentType("application/json")
                .body(Map.of(
                        "origin", "operacion",
                        "title", "Caso incompleto",
                        "priority", "alta",
                        "domain", "regularizacion",
                        "criticality", "MEDIA"))
                .when()
                .post("/api/casos")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void closeCase_withResult_closesAndKeepsTraceability() {
        String caseId = given().contentType("application/json").body(validCasePayload()).when().post("/api/casos").then().statusCode(201).extract().path("id");

        given()
                .contentType("application/json")
                .body(Map.of(
                        "resultadoOperativo", "Caso resuelto con memoria reutilizada",
                        "memoryId", "MEM-12345678",
                        "observaciones", "QA verifico el cierre"))
                .when()
                .post("/api/casos/" + caseId + "/cerrar")
                .then()
                .statusCode(200)
                .body("status", equalTo("CERRADO"))
                .body("closureResult", equalTo("Caso resuelto con memoria reutilizada"))
                .body("closureMemoryId", equalTo("MEM-12345678"))
                .body("closedAt", notNullValue());
    }

    @Test
    @TestSecurity(user = "operator-user", roles = {"memory-operator"})
    void closeCase_withoutResult_returnsConsistentValidationError() {
        String caseId = given().contentType("application/json").body(validCasePayload()).when().post("/api/casos").then().statusCode(201).extract().path("id");

        given()
                .contentType("application/json")
                .body(Map.of("resultadoOperativo", " "))
                .when()
                .post("/api/casos/" + caseId + "/cerrar")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));

        given()
                .when()
                .get("/api/casos/" + caseId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ABIERTO"));
    }

    @Test
    @TestSecurity(user = "auditor-user", roles = {"memory-auditor"})
    void getMissingCase_returnsControlledError() {
        given()
                .when()
                .get("/api/casos/CASO-404")
                .then()
                .statusCode(404)
                .body("code", equalTo("CASE_NOT_FOUND"));
    }

    private Map<String, Object> validCasePayload() {
        return Map.of(
                "origin", "operacion",
                "title", "Incidencia operativa en regularizacion",
                "description", "Caso sin memoria previa asociada",
                "priority", "alta",
                "domain", "regularizacion",
                "criticality", "MEDIA",
                "tags", List.of("incidencia", "regularizacion"),
                "participants", List.of("operador.qa"));
    }
}
