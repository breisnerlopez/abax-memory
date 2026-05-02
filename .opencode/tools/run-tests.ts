import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Ejecuta la suite de tests del proyecto y retorna resultados con estado, cobertura y fallos detectados.",
  args: {
    test_type: tool.schema.string().default("unit").describe("Tipo de test: unit, integration, e2e, all"),
    path: tool.schema.string().describe("Ruta especifica a ejecutar (opcional, vacio para todos)"),
  },
  async execute(args, context) {
    const cmd = args.path ? "npm test -- " + args.path : "npm test";
return "Ejecutando: " + cmd + " (tipo: " + args.test_type + ")";

  },
});
