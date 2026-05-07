import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Escribe una atestación firmada en docs/.attestations/<phase>/<deliverable>.json. Requerida para deliverables con attestation_required:true. Captura git_sha actual, archivos modificados, rol del firmante y opcionalmente resultados de verify-deliverable. Genérico — independiente de qué fase o deliverable específico.",
  args: {
    phase: tool.schema.string().describe("ID de la fase"),
    deliverable: tool.schema.string().describe("ID del deliverable"),
    attestor_role: tool.schema.string().describe("Rol que firma la atestación. Debe coincidir con el `responsible` del deliverable o un rol explícitamente autorizado."),
    notes: tool.schema.string().describe("Observaciones libres del firmante"),
  },
  async execute(args, context) {
    const { readFileSync, existsSync, mkdirSync, writeFileSync } = require("node:fs");
const { join, dirname } = require("node:path");
const { execSync } = require("node:child_process");

const projectDir = process.cwd();
const policiesPath = join(projectDir, ".opencode/policies/abax-policies.json");

// Validate against policies if they exist (soft — works without policies too)
let attestorOk = true;
let attestorReason = "";
if (existsSync(policiesPath)) {
  try {
    const policies = JSON.parse(readFileSync(policiesPath, "utf8"));
    const phase = (policies.phases || []).find((p: any) => p.id === args.phase);
    if (phase) {
      const d = (phase.deliverables || []).find((x: any) => x.id === args.deliverable);
      if (d && d.responsible !== args.attestor_role) {
        attestorOk = false;
        attestorReason = `expected attestor_role=${d.responsible} (deliverable.responsible), got ${args.attestor_role}`;
      }
    }
  } catch {
    // ignore — fail-open on policies parse
  }
}
if (!attestorOk) {
  return JSON.stringify({ ok: false, error: attestorReason });
}

// Capture git context (best-effort; works even without git)
let gitSha = "";
let filesTouched: string[] = [];
try {
  gitSha = execSync("git rev-parse HEAD", { cwd: projectDir, encoding: "utf8" }).trim();
} catch { /* not a git repo */ }
try {
  const diff = execSync("git diff --name-only HEAD", { cwd: projectDir, encoding: "utf8" });
  filesTouched = diff.split("\n").filter((l: string) => l.trim().length > 0);
} catch { /* no diff */ }

const attestation = {
  schema_version: 1,
  phase: args.phase,
  deliverable: args.deliverable,
  attestor_role: args.attestor_role,
  timestamp: new Date().toISOString(),
  git_sha: gitSha,
  files_touched: filesTouched,
  notes: args.notes || "",
};

const attDir = join(projectDir, "docs/.attestations", args.phase);
mkdirSync(attDir, { recursive: true });
const attPath = join(attDir, `${args.deliverable}.json`);
writeFileSync(attPath, JSON.stringify(attestation, null, 2) + "\n", "utf8");

return JSON.stringify({
  ok: true,
  written: `docs/.attestations/${args.phase}/${args.deliverable}.json`,
  attestation,
}, null, 2);

  },
});
