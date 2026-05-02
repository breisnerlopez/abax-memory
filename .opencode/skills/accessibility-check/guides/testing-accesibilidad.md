# testing-accesibilidad

## Estrategia de testing

### Tests automatizados (CI/CD)
- Integrar axe-core con Jest o Cypress para detectar violaciones
  automaticamente en cada build.
- Configurar reglas como umbral minimo: 0 violaciones criticas.
- Usar eslint-plugin-jsx-a11y para capturar errores en tiempo
  de desarrollo.

### Tests manuales
- Navegar toda la aplicacion usando solo teclado.
- Probar con al menos un lector de pantalla (NVDA o VoiceOver).
- Verificar contraste con el inspector de color del navegador.
- Probar con zoom al 200% y 400%.
- Desactivar CSS y verificar que el orden de lectura sea logico.

### Tests con usuarios reales
- Incluir personas con discapacidad en las sesiones de usabilidad.
- Probar con tecnologias asistivas reales en dispositivos reales.
- Documentar hallazgos y priorizarlos por impacto.
