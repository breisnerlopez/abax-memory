import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Genera presentaciones estructuradas en formato HTML autonomo con estilos CSS inline, estructura de slides, contenido adaptado al tipo de presentacion y audiencia objetivo. Usa el Design System del proyecto para consistencia visual.",
  args: {
    title: tool.schema.string().describe("Titulo de la presentacion"),
    presentation_type: tool.schema.string().default("status").describe("Tipo: status, decision, demo, retrospective, kickoff, go-live"),
    audience: tool.schema.string().default("executive").describe("Audiencia objetivo: executive, technical, end-users, all-stakeholders"),
    phase: tool.schema.string().describe("Fase del proyecto asociada a la presentacion"),
    milestone: tool.schema.string().describe("Hito del proyecto asociado (si aplica)"),
    content: tool.schema.string().describe("Contenido principal de los slides en Markdown"),
  },
  async execute(args, context) {
    const date = new Date().toISOString().split("T")[0];
const typeLabels: Record<string, string> = {
  status: "Status Update",
  decision: "Decision / Aprobacion",
  demo: "Demostracion",
  retrospective: "Retrospectiva / Cierre",
  kickoff: "Kickoff",
  "go-live": "Go-Live Readiness",
};
const label = typeLabels[args.presentation_type] || args.presentation_type;

let output = `# ${args.title}\n\n`;
output += `| Campo | Valor |\n|---|---|\n`;
output += `| Tipo | ${label} |\n`;
output += `| Audiencia | ${args.audience} |\n`;
output += `| Fecha | ${date} |\n`;
if (args.phase) output += `| Fase | ${args.phase} |\n`;
if (args.milestone) output += `| Hito | ${args.milestone} |\n`;
output += `| Estado | Borrador |\n\n---\n\n`;

output += `## Agenda\n\n`;
output += `1. Contexto\n2. Contenido principal\n3. Proximos pasos\n4. Preguntas / Decision\n\n---\n\n`;

output += args.content + "\n\n---\n\n";

output += `## Proximos Pasos\n\n| Accion | Responsable | Fecha |\n|---|---|---|\n| [Pendiente] | [Nombre] | [Fecha] |\n\n`;

if (args.presentation_type === "decision") {
  output += `## Decision Solicitada\n\n> **Pregunta**: [Insertar pregunta explicita de aprobacion]\n\n`;
  output += `| Opcion | Descripcion | Recomendacion |\n|---|---|---|\n| A | [Opcion A] | **Recomendada** |\n| B | [Opcion B] | Alternativa |\n`;
}

return output;

  },
});
