import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Genera y valida diagramas Mermaid a partir de descripciones textuales de procesos, arquitectura y flujos.",
  args: {
    description: tool.schema.string().describe("Descripcion o codigo Mermaid del diagrama a generar"),
    diagram_type: tool.schema.string().default("flowchart").describe("Tipo de diagrama: flowchart, sequence, classDiagram, erDiagram, gantt, stateDiagram"),
  },
  async execute(args, context) {
    // Runtime defaults — el plugin OpenCode no aplica .default() del schema
// cuando el LLM omite el arg (incidente create-presentation 0.1.32).
const validTypes = ["flowchart", "sequence", "classDiagram", "erDiagram", "gantt", "stateDiagram"];
const type = validTypes.includes(args.diagram_type) ? args.diagram_type : "flowchart";
const description = args.description || "(sin descripcion)";
return "```mermaid\n" + type + "\n" + description + "\n```";

  },
});
