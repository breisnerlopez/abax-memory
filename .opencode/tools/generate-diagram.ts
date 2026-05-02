import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Genera y valida diagramas Mermaid a partir de descripciones textuales de procesos, arquitectura y flujos.",
  args: {
    description: tool.schema.string().describe("Descripcion o codigo Mermaid del diagrama a generar"),
    diagram_type: tool.schema.string().default("flowchart").describe("Tipo de diagrama: flowchart, sequence, classDiagram, erDiagram, gantt, stateDiagram"),
  },
  async execute(args, context) {
    const validTypes = ["flowchart", "sequence", "classDiagram", "erDiagram", "gantt", "stateDiagram"];
const type = validTypes.includes(args.diagram_type) ? args.diagram_type : "flowchart";
return "```mermaid\n" + type + "\n" + args.description + "\n```";

  },
});
