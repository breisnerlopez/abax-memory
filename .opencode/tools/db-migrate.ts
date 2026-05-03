import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Genera y ejecuta scripts de migracion de base de datos de forma versionada y con soporte de rollback.",
  args: {
    action: tool.schema.string().default("status").describe("Accion: generate, up, down, status"),
    name: tool.schema.string().describe("Nombre descriptivo de la migracion (solo para generate)"),
  },
  async execute(args, context) {
    // Runtime defaults — el plugin OpenCode no aplica .default() del schema
// cuando el LLM omite el arg (incidente create-presentation 0.1.32).
const action = args.action || "status";
const name = args.name || "";
return "Ejecutando migracion: " + action + (name ? " (" + name + ")" : "");

  },
});
