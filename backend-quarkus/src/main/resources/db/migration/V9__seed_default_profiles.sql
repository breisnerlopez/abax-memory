-- ============================================================
-- Abax-Memory v2.0.0 — Migration V9
-- Seeds the default domain profiles: ops, agent, business.
-- Each profile is a JSONB config that specializes the generic
-- memory engine for a specific domain.
-- PostgreSQL 16+
-- ============================================================

-- Profile: ops — IT Operations (SRE, DevOps, Support)
INSERT INTO profiles (id, name, version, description, config, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000001'::UUID,
    'ops',
    '1.0',
    'IT Operations profile: incident management, runbooks, deployments, and postmortems',
    '{
        "recommendedKinds": ["INCIDENT", "PROCEDURE", "DECISION", "KNOWLEDGE"],
        "defaultSensitivity": "INTERNAL",
        "defaultConfidence": 0.7,
        "suggestedTags": ["incident", "runbook", "alert", "maintenance", "postmortem", "deployment"],
        "suggestedTopics": ["networking", "database", "kubernetes", "monitoring", "security"],
        "extraMetadataFields": [
            {"name": "affectedService", "type": "string", "label": "Affected Service"},
            {"name": "remediationSteps", "type": "text", "label": "Remediation Steps"},
            {"name": "rootCause", "type": "text", "label": "Root Cause"},
            {"name": "incidentSeverity", "type": "string", "label": "Incident Severity"},
            {"name": "downtimeMinutes", "type": "number", "label": "Downtime (minutes)"}
        ],
        "lifecycleDefaults": {
            "initialStatus": "DRAFT",
            "requireReviewThreshold": {"importance": 0.7, "sensitivities": ["CONFIDENTIAL", "RESTRICTED"]}
        }
    }',
    TRUE,
    NOW(),
    NOW()
);

-- Profile: agent — Conversational AI Agent Memory
INSERT INTO profiles (id, name, version, description, config, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000002'::UUID,
    'agent',
    '1.0',
    'AI Agent conversational memory: user facts, preferences, session context, decision history',
    '{
        "recommendedKinds": ["AGENT_MEMORY", "DECISION", "ENTITY", "KNOWLEDGE"],
        "defaultSensitivity": "INTERNAL",
        "defaultConfidence": 0.5,
        "suggestedTags": ["user-fact", "user-preference", "session-context", "decision-history", "interaction"],
        "suggestedTopics": ["personal-info", "communication-style", "task-history", "tool-usage"],
        "extraMetadataFields": [
            {"name": "interactionType", "type": "string", "label": "Interaction Type"},
            {"name": "turnNumber", "type": "number", "label": "Turn Number"},
            {"name": "contextWindow", "type": "string", "label": "Context Window"},
            {"name": "agentName", "type": "string", "label": "Agent Name"}
        ],
        "lifecycleDefaults": {
            "initialStatus": "DRAFT",
            "requireReviewThreshold": {"importance": 0.8, "sensitivities": ["RESTRICTED"]}
        }
    }',
    TRUE,
    NOW(),
    NOW()
);

-- Profile: business — CRM / Legal / Finance / Product
INSERT INTO profiles (id, name, version, description, config, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000003'::UUID,
    'business',
    '1.0',
    'Business profile: client management, contracts, meetings, opportunities, decisions',
    '{
        "recommendedKinds": ["ENTITY", "DOCUMENT", "DECISION", "CUSTOM", "KNOWLEDGE"],
        "defaultSensitivity": "CONFIDENTIAL",
        "defaultConfidence": 0.5,
        "suggestedTags": ["client", "contract", "meeting", "opportunity", "proposal", "invoice"],
        "suggestedTopics": ["sales", "legal", "finance", "product", "support"],
        "extraMetadataFields": [
            {"name": "clientName", "type": "string", "label": "Client Name"},
            {"name": "contractId", "type": "string", "label": "Contract ID"},
            {"name": "opportunityValue", "type": "number", "label": "Opportunity Value"},
            {"name": "meetingDate", "type": "date", "label": "Meeting Date"},
            {"name": "attendees", "type": "text", "label": "Attendees"},
            {"name": "dealStage", "type": "string", "label": "Deal Stage"}
        ],
        "lifecycleDefaults": {
            "initialStatus": "DRAFT",
            "requireReviewThreshold": {"importance": 0.7, "sensitivities": ["CONFIDENTIAL", "RESTRICTED"]}
        }
    }',
    TRUE,
    NOW(),
    NOW()
);
