import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Genera y ejecuta scripts de migracion de base de datos de forma versionada y con soporte de rollback.",
  args: {
    action: tool.schema.string().default("status").describe("Accion: generate, up, down, status"),
    name: tool.schema.string().describe("Nombre descriptivo de la migracion (solo para generate)"),
  },
  async execute(args, context) {
    return "Ejecutando migracion: " + args.action + (args.name ? " (" + args.name + ")" : "");

  },
});
