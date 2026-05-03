import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Crea documentos Markdown estructurados con metadata, secciones y formato estandarizado del proyecto.",
  args: {
    title: tool.schema.string().describe("Titulo del documento"),
    doc_type: tool.schema.string().default("functional").describe("Tipo: functional, technical, test-plan, deployment, risk, change-request"),
    content: tool.schema.string().describe("Contenido principal del documento en Markdown"),
  },
  async execute(args, context) {
    // Runtime defaults — el plugin OpenCode no aplica los .default() del schema
// cuando el LLM omite el arg (incidente create-presentation 2026-05-03 0.1.32).
const title = args.title || "(sin titulo)";
const docType = args.doc_type || "functional";
const content = args.content || "";
const header = "# " + title + "\n\n";
const meta = "| Campo | Valor |\n|---|---|\n| Tipo | " + docType + " |\n| Fecha | " + new Date().toISOString().split("T")[0] + " |\n| Estado | Borrador |\n\n---\n\n";
return header + meta + content;

  },
});
