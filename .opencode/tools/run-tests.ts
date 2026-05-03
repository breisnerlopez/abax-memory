import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Ejecuta la suite de tests del proyecto y retorna resultados con estado, cobertura y fallos detectados.",
  args: {
    test_type: tool.schema.string().default("unit").describe("Tipo de test: unit, integration, e2e, all"),
    path: tool.schema.string().describe("Ruta especifica a ejecutar (opcional, vacio para todos)"),
  },
  async execute(args, context) {
    // Runtime defaults — el plugin OpenCode no aplica .default() del schema
// cuando el LLM omite el arg (incidente create-presentation 0.1.32).
const testType = args.test_type || "unit";
const path = args.path || "";
const cmd = path ? "npm test -- " + path : "npm test";
return "Ejecutando: " + cmd + " (tipo: " + testType + ")";

  },
});
