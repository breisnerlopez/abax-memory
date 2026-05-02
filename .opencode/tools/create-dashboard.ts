import { tool } from "@opencode-ai/plugin";

export default tool({
  description: "Genera dashboards de seguimiento del proyecto en formato Mermaid y tablas Markdown, incluyendo diagramas Gantt, graficos de avance, semaforos de estado y tableros de control de entregables.",
  args: {
    dashboard_type: tool.schema.string().default("status-summary").describe("Tipo: gantt, burndown, deliverables, status-summary, kanban"),
    project_name: tool.schema.string().describe("Nombre del proyecto"),
    data: tool.schema.string().describe("Datos estructurados para el dashboard (tareas, fechas, estados)"),
    report_date: tool.schema.string().describe("Fecha del reporte (YYYY-MM-DD)"),
  },
  async execute(args, context) {
    const date = args.report_date || new Date().toISOString().split("T")[0];
let output = `# Dashboard: ${args.project_name}\n`;
output += `> Generado: ${date} | Tipo: ${args.dashboard_type}\n\n`;

if (args.dashboard_type === "gantt") {
  output += "```mermaid\ngantt\n";
  output += `  title ${args.project_name} - Cronograma\n`;
  output += "  dateFormat YYYY-MM-DD\n";
  output += args.data + "\n";
  output += "```\n";
} else if (args.dashboard_type === "status-summary") {
  output += "## Resumen Ejecutivo\n\n";
  output += "| Indicador | Valor | Semaforo |\n|---|---|---|\n";
  output += args.data + "\n";
} else if (args.dashboard_type === "deliverables") {
  output += "## Control de Entregables\n\n";
  output += "| Fase | Total | Aprobados | En Progreso | Pendientes | % |\n|---|---|---|---|---|---|\n";
  output += args.data + "\n";
} else if (args.dashboard_type === "burndown") {
  output += "```mermaid\nxychart-beta\n";
  output += `  title "${args.project_name} - Burndown"\n`;
  output += "  x-axis [S1, S2, S3, S4, S5, S6, S7, S8]\n";
  output += args.data + "\n";
  output += "```\n";
} else if (args.dashboard_type === "kanban") {
  output += "## Tablero Kanban\n\n";
  output += args.data + "\n";
}

output += "\n---\n*Dashboard generado automaticamente*\n";
return output;

  },
});
