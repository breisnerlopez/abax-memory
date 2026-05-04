package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import com.abax.memory.domain.service.MemoryService;
import com.abax.memory.infrastructure.persistence.DomainProfileEntity;
import com.abax.memory.infrastructure.persistence.TenantConfigEntity;
import com.abax.memory.test.H2TestProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Domain profile tests for EP-002: Domain Profiles.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>Creating a memory without kind applies the profile default.</li>
 *   <li>The 3 seed profiles (ops, agent, business) can be created and used.</li>
 *   <li>Changing the tenant's profile affects subsequent memory defaults.</li>
 * </ul>
 *
 * <p>References: HU-002.1, HU-002.2, §3 of functional spec</p>
 */
@QuarkusTest
@TestProfile(H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("EP-002: Domain Profile Tests")
class DomainProfileTest {

    private static final String TENANT_OPS = "tenant-ops";
    private static final String TENANT_AGENT = "tenant-agent";
    private static final String TENANT_BUSINESS = "tenant-business";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    MemoryService memoryService;

    // ── Helper to seed a profile ────────────────────────────────────

    @Transactional
    DomainProfileEntity seedProfile(String name, String description, String configJson) {
        var entity = new DomainProfileEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setVersion("1.0");
        entity.setDescription(description);
        entity.setConfig(configJson);
        entity.setActive(true);
        entity.persist();
        return entity;
    }

    @Transactional
    void assignProfileToTenant(String tenantId, UUID profileId) {
        var config = TenantConfigEntity.findByTenantId(tenantId);
        if (config == null) {
            config = new TenantConfigEntity();
            config.setTenantId(tenantId);
            config.setProfileId(profileId);
            config.setActive(true);
            config.persist();
        } else {
            config.setProfileId(profileId);
            config.persist();
        }
    }

    // ── D3.1: Create memory without kind applies profile default ────

    @Test
    @Order(1)
    @DisplayName("D3.1 — Creating memory without kind applies profile default (ops: INCIDENT)")
    @Transactional
    void createMemory_withoutKind_appliesProfileDefault() {
        // Seed the ops profile
        var opsProfile = seedProfile("ops",
                "IT Operations profile",
                """
                {
                    "recommendedKinds": ["INCIDENT", "PROCEDURE", "DECISION", "KNOWLEDGE"],
                    "defaultSensitivity": "INTERNAL",
                    "defaultConfidence": 0.7
                }
                """);

        // Assign to tenant
        assignProfileToTenant(TENANT_OPS, opsProfile.getId());

        // Create memory without kind, sensitivity, confidence
        var request = new CreateMemoryRequest(
                "Ops Default Test", "Testing profile defaults.",
                null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT_OPS);

        // Verify defaults from ops profile
        assertThat(created.kind()).isEqualTo(MemoryKind.INCIDENT); // first recommended kind
        assertThat(created.sensitivityLevel()).isEqualTo(SensitivityLevel.INTERNAL);
        assertThat(created.confidence()).isEqualTo(0.7);
    }

    // ── D3.2: Agent profile defaults ────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("D3.2 — Agent profile applies AGENT_MEMORY as default kind")
    @Transactional
    void agentProfile_appliesAgentMemoryDefault() {
        // Seed the agent profile
        var agentProfile = seedProfile("agent",
                "AI Agent conversational memory",
                """
                {
                    "recommendedKinds": ["AGENT_MEMORY", "DECISION", "ENTITY", "KNOWLEDGE"],
                    "defaultSensitivity": "INTERNAL",
                    "defaultConfidence": 0.5
                }
                """);

        assignProfileToTenant(TENANT_AGENT, agentProfile.getId());

        var request = new CreateMemoryRequest(
                "Agent Default Test", "Testing agent profile defaults.",
                null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT_AGENT);

        assertThat(created.kind()).isEqualTo(MemoryKind.AGENT_MEMORY);
        assertThat(created.sensitivityLevel()).isEqualTo(SensitivityLevel.INTERNAL);
        assertThat(created.confidence()).isEqualTo(0.5);
    }

    // ── D3.3: Business profile defaults ─────────────────────────────

    @Test
    @Order(3)
    @DisplayName("D3.3 — Business profile applies ENTITY as default kind and CONFIDENTIAL sensitivity")
    @Transactional
    void businessProfile_appliesEntityDefault() {
        // Seed the business profile
        var businessProfile = seedProfile("business",
                "Business profile: CRM / Legal / Finance",
                """
                {
                    "recommendedKinds": ["ENTITY", "DOCUMENT", "DECISION", "CUSTOM", "KNOWLEDGE"],
                    "defaultSensitivity": "CONFIDENTIAL",
                    "defaultConfidence": 0.5
                }
                """);

        assignProfileToTenant(TENANT_BUSINESS, businessProfile.getId());

        var request = new CreateMemoryRequest(
                "Business Default Test", "Testing business profile defaults.",
                null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, TENANT_BUSINESS);

        assertThat(created.kind()).isEqualTo(MemoryKind.ENTITY);
        assertThat(created.sensitivityLevel()).isEqualTo(SensitivityLevel.CONFIDENTIAL);
        assertThat(created.confidence()).isEqualTo(0.5);
    }

    // ── D3.4: Explicit values override profile defaults ─────────────

    @Test
    @Order(4)
    @DisplayName("D3.4 — Explicit values override profile defaults")
    @Transactional
    void explicitValues_overrideProfileDefaults() {
        var opsProfile = seedProfile("ops2",
                "Ops profile for override test",
                """
                {
                    "recommendedKinds": ["INCIDENT", "PROCEDURE"],
                    "defaultSensitivity": "INTERNAL",
                    "defaultConfidence": 0.7
                }
                """);
        assignProfileToTenant(TENANT_OPS, opsProfile.getId());

        // Provide explicit kind, sensitivity, and confidence
        var request = new CreateMemoryRequest(
                "Explicit Override Test", "Overriding profile defaults.",
                MemoryKind.DOCUMENT, null, SensitivityLevel.RESTRICTED,
                null, null, 0.99, null);
        MemoryResponse created = memoryService.createV2(request, TENANT_OPS);

        // Explicit values must be preserved, not overwritten by profile
        assertThat(created.kind()).isEqualTo(MemoryKind.DOCUMENT);
        assertThat(created.sensitivityLevel()).isEqualTo(SensitivityLevel.RESTRICTED);
        assertThat(created.confidence()).isEqualTo(0.99);
    }

    // ── D3.5: Changing tenant profile affects defaults ──────────────

    @Test
    @Order(5)
    @DisplayName("D3.5 — Changing tenant profile affects subsequent memory defaults")
    @Transactional
    void changingProfile_affectsSubsequentDefaults() {
        // First assign agent profile
        var agentProfile = seedProfile("agent-switch",
                "Agent profile for switch test",
                """
                {
                    "recommendedKinds": ["AGENT_MEMORY", "DECISION"],
                    "defaultSensitivity": "INTERNAL",
                    "defaultConfidence": 0.5
                }
                """);
        assignProfileToTenant("tenant-switch", agentProfile.getId());

        // Create with agent profile
        var request1 = new CreateMemoryRequest(
                "Before Switch", "Agent profile memory.",
                null, null, null, null, null, null, null);
        MemoryResponse before = memoryService.createV2(request1, "tenant-switch");
        assertThat(before.kind()).isEqualTo(MemoryKind.AGENT_MEMORY);
        assertThat(before.sensitivityLevel()).isEqualTo(SensitivityLevel.INTERNAL);

        // Now switch to business profile
        var businessProfile = seedProfile("business-switch",
                "Business profile for switch test",
                """
                {
                    "recommendedKinds": ["ENTITY", "DOCUMENT"],
                    "defaultSensitivity": "CONFIDENTIAL",
                    "defaultConfidence": 0.5
                }
                """);
        assignProfileToTenant("tenant-switch", businessProfile.getId());

        // Create with business profile defaults
        var request2 = new CreateMemoryRequest(
                "After Switch", "Business profile memory.",
                null, null, null, null, null, null, null);
        MemoryResponse after = memoryService.createV2(request2, "tenant-switch");

        assertThat(after.kind()).isEqualTo(MemoryKind.ENTITY); // changed from AGENT_MEMORY
        assertThat(after.sensitivityLevel()).isEqualTo(SensitivityLevel.CONFIDENTIAL); // changed from INTERNAL
    }

    // ── D3.6: Tenant without profile uses system defaults ───────────

    @Test
    @Order(6)
    @DisplayName("D3.6 — Tenant without profile gets system defaults (KNOWLEDGE, INTERNAL, 0.5)")
    @Transactional
    void tenantWithoutProfile_usesSystemDefaults() {
        var request = new CreateMemoryRequest(
                "No Profile Test", "No tenant config set up.",
                null, null, null, null, null, null, null);
        MemoryResponse created = memoryService.createV2(request, "tenant-no-config");

        assertThat(created.kind()).isEqualTo(MemoryKind.KNOWLEDGE);
        assertThat(created.sensitivityLevel()).isEqualTo(SensitivityLevel.INTERNAL);
        assertThat(created.confidence()).isEqualTo(0.5);
    }

    // ── D3.7: Three seed profiles exist and are queryable ───────────

    @Test
    @Order(7)
    @DisplayName("D3.7 — All 3 seed profiles can be created and queried")
    @Transactional
    void threeProfiles_areQueryable() {
        // Seed all three
        seedProfile("ops-seed",
                "Ops",
                """
                {"recommendedKinds":["INCIDENT","PROCEDURE"],"defaultSensitivity":"INTERNAL","defaultConfidence":0.7}
                """);
        seedProfile("agent-seed",
                "Agent",
                """
                {"recommendedKinds":["AGENT_MEMORY","DECISION"],"defaultSensitivity":"INTERNAL","defaultConfidence":0.5}
                """);
        seedProfile("business-seed",
                "Business",
                """
                {"recommendedKinds":["ENTITY","DOCUMENT"],"defaultSensitivity":"CONFIDENTIAL","defaultConfidence":0.5}
                """);

        // Query all active profiles
        var profiles = DomainProfileEntity.listActive();

        assertThat(profiles).hasSizeGreaterThanOrEqualTo(3);
        assertThat(profiles.stream().map(p -> ((DomainProfileEntity) p).getName()))
                .contains("ops-seed", "agent-seed", "business-seed");

        // Query by name
        var ops = DomainProfileEntity.findByName("ops-seed");
        assertThat(ops).isNotNull();
        assertThat(ops.getDescription()).isEqualTo("Ops");

        var agent = DomainProfileEntity.findByName("agent-seed");
        assertThat(agent).isNotNull();

        var business = DomainProfileEntity.findByName("business-seed");
        assertThat(business).isNotNull();
    }
}
