# Publicación de Presentaciones en GitHub Pages
- **Fase**: Fase 9 — Cierre
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-02
- **Estado**: Completado (con acción manual pendiente)
---

## Resumen Ejecutivo

Se creó el índice principal (`docs/index.html`) para GitHub Pages usando el Design System del proyecto, y se subió al repositorio. La habilitación de GitHub Pages requiere una acción manual del owner del repositorio debido a que el token de API de `gh` CLI expiró y no puede renovarse en este entorno no-interactivo.

---

## PARTE 1 — Índice de Presentaciones

### Archivo creado
- **Ruta**: `docs/index.html`
- **Tamaño**: 508 líneas
- **Commit**: `9e641f6` — `feat: agregar indice de presentaciones para GitHub Pages (Fase 9 - Cierre)`
- **Push**: Confirmado a `origin/main`

### Características del índice
- Usa el mismo **Design System CSS** del template base (variables, tipografía, colores, badges, sombras)
- **8 tarjetas interactivas** con hover effects y accesibilidad
- Cada tarjeta incluye:
  - Número y nombre de la fase
  - Título de la presentación (con enlace)
  - Descripción breve
  - Badge de estado (Aprobada / Completada)
  - Botón "Ver" para abrir la presentación
- Diseño **responsive** (mobile, tablet, desktop)
- Estilos **print** para exportación limpia a PDF
- Enlaces **relativos** a cada presentación

### Presentaciones indexadas

| # | Fase | Archivo | Estado |
|---|------|---------|--------|
| 1 | Fase 0 · Descubrimiento | `entregables/fase-0-descubrimiento/presentacion-descubrimiento.html` | Aprobada |
| 2 | Fase 1 · Inicio | `entregables/fase-1-inicio/presentacion-kickoff.html` | Aprobada |
| 3 | Fase 2 · Análisis | `entregables/fase-2-analisis/presentacion-propuesta-funcional.html` | Aprobada |
| 4 | Fase 3 · Diseño Técnico | `entregables/fase-3-diseno-tecnico/presentacion-arquitectura.html` | Aprobada |
| 5 | Fase 4 · Construcción | `entregables/fase-4-construccion/presentacion-avance.html` | Completada |
| 6 | Fase 6 · UAT | `entregables/fase-6-uat/presentacion-resultados-uat.html` | Completada |
| 7 | Fase 7 · Despliegue | `entregables/fase-7-despliegue/presentacion-go-live.html` | Completada |
| 8 | Fase 9 · Cierre | `entregables/fase-9-cierre/presentacion-cierre.html` | Completada |

---

## PARTE 2 — Configuración de GitHub Pages

### Intento realizado
```bash
gh api repos/breisnerlopez/abax-memory/pages -X POST \
  -f "source[branch]=main" \
  -f "source[path]=/docs"
```

### Resultado
```
HTTP 401 — Bad credentials
```

### Diagnóstico

| Elemento | Estado |
|----------|--------|
| `gh` CLI instalado | ✅ v2.45.0 |
| Autenticación SSH (git) | ✅ Funcional — push/pull exitoso |
| Token OAuth `gh` CLI | ❌ Expirado / inválido |
| `gh auth refresh` | ❌ Requiere browser interactivo (no disponible en este entorno) |
| Visibilidad del repositorio | 🔒 **Privado** (`"visibility":"PRIVATE"`) |

### Causa raíz
El token almacenado en `~/.config/gh/hosts.yml` expiró. La renovación requiere abrir un navegador e ingresar un código de verificación (device flow), lo cual no es posible en este entorno de ejecución.

### Nota sobre repositorio privado
El repositorio `breisnerlopez/abax-memory` es **privado**. GitHub Pages en repositorios privados:
- **GitHub Free**: Permite publicar Pages, pero el sitio será **público** (accesible para cualquier persona con la URL).
- **GitHub Pro/Team/Enterprise**: Permite Pages privado (solo visible para colaboradores).

Esto debe considerarse antes de habilitar Pages si se requiere confidencialidad.

---

## PARTE 3 — Verificación y URL

### URL esperada (una vez habilitado Pages)
```
https://breisnerlopez.github.io/abax-memory/
```

### URLs de presentaciones individuales
```
https://breisnerlopez.github.io/abax-memory/entregables/fase-0-descubrimiento/presentacion-descubrimiento.html
https://breisnerlopez.github.io/abax-memory/entregables/fase-1-inicio/presentacion-kickoff.html
https://breisnerlopez.github.io/abax-memory/entregables/fase-2-analisis/presentacion-propuesta-funcional.html
https://breisnerlopez.github.io/abax-memory/entregables/fase-3-diseno-tecnico/presentacion-arquitectura.html
https://breisnerlopez.github.io/abax-memory/entregables/fase-4-construccion/presentacion-avance.html
https://breisnerlopez.github.io/abax-memory/entregables/fase-6-uat/presentacion-resultados-uat.html
https://breisnerlopez.github.io/abax-memory/entregables/fase-7-despliegue/presentacion-go-live.html
https://breisnerlopez.github.io/abax-memory/entregables/fase-9-cierre/presentacion-cierre.html
```

### Tiempo de despliegue
Una vez habilitado, GitHub Pages típicamente tarda entre **1 y 5 minutos** en el primer despliegue.

---

## Acción manual requerida

El owner del repositorio debe completar uno de estos pasos:

### Opción A — Interfaz web de GitHub (recomendado)
1. Ir a: https://github.com/breisnerlopez/abax-memory/settings/pages
2. En **"Build and deployment"**:
   - **Source**: `Deploy from a branch`
   - **Branch**: `main`
   - **Folder**: `/docs`
3. Clic en **Save**
4. Esperar ~1-3 minutos. La URL aparecerá en esa misma página.

### Opción B — Reautenticar gh CLI y ejecutar comando
```bash
gh auth login
# Seguir el flujo interactivo

# Luego ejecutar:
gh api repos/breisnerlopez/abax-memory/pages -X POST \
  -f "source[branch]=main" \
  -f "source[path]=/docs"
```

### Verificación post-deploy
```bash
# Verificar que Pages está activo
gh api repos/breisnerlopez/abax-memory/pages

# Respuesta esperada:
# {
#   "url": "https://breisnerlopez.github.io/abax-memory/",
#   "status": "built",
#   "cname": null,
#   ...
# }
```

---

## Checklist de entrega

| Entregable | Estado |
|------------|--------|
| `docs/index.html` creado con Design System | ✅ Completado |
| 8 presentaciones indexadas con enlaces relativos | ✅ Completado |
| Commit y push a `main` | ✅ Completado |
| GitHub Pages habilitado via API | ❌ Pendiente (token expirado) |
| Verificación de URL | ⏳ Pendiente de habilitación |
