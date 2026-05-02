import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Ejecuta linters y analizadores estaticos sobre el codigo fuente para detectar problemas de estilo y calidad.",
  args: {
    path: tool.schema.string().default(".").describe("Ruta del archivo o directorio a analizar"),
    fix: tool.schema.boolean().describe("Intentar corregir automaticamente los problemas encontrados"),
  },
  async execute(args, context) {
    const fixFlag = args.fix ? " --fix" : "";
return "Ejecutando: npm run lint -- " + args.path + fixFlag;

  },
});
