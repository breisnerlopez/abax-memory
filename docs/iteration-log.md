# Iteration Log — Abax-Memory

- **Fase**: v2.0.0 — Inception (pre-F0)
- **Responsable**: project-manager (registro) / orquestador + usuario (decisión)
- **Fecha**: 2026-05-03
- **Estado**: Iteración mayor iniciada — Estrategia documentada

---

## 1. Contexto del Proyecto v1.0.0

### Estado al momento de la iteración

| Indicador | Valor |
|---|---|
| Versión | v1.0.0 |
| Estado | **CERRADO** (2026-05-02) |
| Fases completadas | 9/9 (F0 — Descubrimiento a F9 — Cierre) |
| Entregables completados | 42 |
| Duración total | 2 días (2026-05-01 a 2026-05-02) |
| Producto | PMOA — Memoria Operativa para IT Operations |
| Stack | Backend Quarkus 3.15.3 + PostgreSQL 16.13 + Qdrant 1.17.1 + Keycloak 26.1.0 + OpenAI (`text-embedding-3-large`, `gpt-4o-mini`) |
| Calidad final | 61/61 CA UAT (100%), 49/49 QA, 54 tests BUILD SUCCESS, 26/26 estabilización, 0 defectos críticos abiertos |
| Release | Publicado en GitHub + GHCR: `ghcr.io/breisnerlopez/abax-memory:latest` |
| Dominio | IT Operations (memoria operativa para equipos de infraestructura y soporte) |
| Audiencia | Operadores IT, revisores, administradores, auditores |

### Estructura de documentación v1 (pre-iteración)

```
docs/
├── bitacora.md
├── registro-entregables.md
├── design-system/
├── index.html
└── entregables/
    ├── fase-0-descubrimiento/
    ├── fase-1-inicio/
    ├── fase-2-analisis/
    ├── fase-3-diseno-tecnico/
    ├── fase-4-construccion/
    ├── fase-5-pruebas-qa/
    ├── fase-5/
    ├── fase-6-uat/
    ├── fase-7-despliegue/
    ├── fase-8-estabilizacion/
    ├── fase-9-cierre/
    ├── fase-R2-correccion/
    ├── fase-R2/
    └── r2-mcp/
```

---

## 2. Gatilladores de Iteration Strategy

La skill `iteration-strategy` fue activada por las siguientes condiciones simultáneas:

### Condición 1 — Proyecto con historia previa

| Señal | Evidencia |
|---|---|
| `docs/bitacora.md` existe | ✅ — 796 líneas, última entrada: «PROYECTO CERRADO, Fase 9/9» |
| `docs/entregables/fase-9-cierre/` existe | ✅ — Informe de cierre + lecciones aprendidas |
| Fase 9 completada | ✅ — Gate CERRADO el 2026-05-02 |
| Release publicado | ✅ — v1.0.0 en GitHub, tag aplicado, imagen en GHCR |

### Condición 2 — Nueva iteración mayor de alcance significativo

| Señal | Evidencia |
|---|---|
| Usuario solicita "motor de memoria genérica multi-dominio" | ✅ — Cambio de dominio: IT Operations → Multi-dominio |
| Cambio de audiencia | ✅ — De equipos IT a cualquier industria/vertical |
| Cambio de arquitectura interna | ✅ — Perfiles de dominio dinámicos, motor genérico |
| Esfuerzo estimado comparable a F0-F9 original | ✅ — Nueva fase 0, backlog independiente |

### Otros gatilladores contextuales

- La propuesta implica re-arquitectura del motor de memoria para soportar dominios arbitrarios mediante perfiles.
- El modelo de datos, las reglas de negocio y los contratos API cambian de vocabulario fijo (IT ops) a vocabulario dinámico (perfil de dominio).
- La convención de código requiere migración a **English-Only Internals** (skill `code-naming-convention`), lo cual afecta endpoints, modelos y documentación técnica.

---

## 3. Decisión Tomada

### Estrategia seleccionada: **A — Folder por release**

| Campo | Valor |
|---|---|
| Estrategia | A — Folder por release |
| Decidida por | Usuario (sponsor del proyecto) |
| Fecha de decisión | 2026-05-03 |
| Registrada por | project-manager (orquestador) |
| Aplica a | Toda la iteración v2.0.0 |

### Justificación

1. **Cambio significativo de alcance**: v1.0.0 era PMOA (memoria operativa para IT Operations). v2.0.0 evoluciona a un motor de memoria genérica multi-dominio. No es un refinamiento incremental — es una transformación del producto.

2. **Audiencia distinta**: v1 apuntaba a equipos de IT (operadores, revisores, administradores de infraestructura). v2 apunta a cualquier industria o vertical mediante perfiles de dominio configurables.

3. **Arquitectura interna evoluciona**: El motor de memoria debe soportar dominios arbitrarios con esquemas de metadatos, reglas de validación y flujos de aprobación definidos por perfil. Esto implica cambios profundos en el modelo de datos, la lógica de extracción y la API.

4. **Convención English-Only**: v2 adopta la skill `code-naming-convention` de forma estricta (todos los identificadores en inglés). Los endpoints, modelos, tablas y columnas deben migrar de español a inglés. Mantener v1 y v2 en el mismo folder crearía confusión de idioma en la documentación.

5. **Preservación intacta del histórico v1**: La documentación completa de v1 (42 entregables, 9 fases) representa trabajo aprobado. Separar por release garantiza trazabilidad sin riesgo de sobreescritura accidental.

### Estrategias descartadas

| Estrategia | Motivo del descarte |
|---|---|
| B — Bloque "## Cambios v2" | No aplica: v2 no es refinamiento incremental. Cambia dominio, audiencia, arquitectura y convención de idioma. Agregar bloques de diff al final de 42+ archivos es frágil e ilegible. |
| C — Archivar y reescribir | Demasiado disruptivo. El equipo puede necesitar consultar v1 durante el desarrollo de v2. Archivar v1 a `.archive/` oculta contexto útil. |
| D — Branch git | La iteración no es experimental — el usuario confirmó el compromiso con v2 como producto. Branch es para prototipos o R&D, no para releases confirmadas. |

---

## 4. Estructura de Folders Resultante

### Organización post-migración

```
docs/
├── bitacora.md                              # Mantiene histórico v1 + registra inicio v2
├── registro-entregables.md                  # Mantiene histórico v1 + registrará v2
├── iteration-log.md                         # ESTE ARCHIVO — bitácora de decisiones de iteración
├── release-mapping.md                       # Relación v1 ↔ v2 (a crear en F0 de v2)
├── design-system/
│   └── ...
├── index.html
├── .archive/                                # Vacío — reservado para futuras iteraciones
└── entregables/
    ├── v1/                                  # ← CONTENIDO EXISTENTE MOVIDO AQUÍ
    │   ├── fase-0-descubrimiento/
    │   ├── fase-1-inicio/
    │   ├── fase-2-analisis/
    │   ├── fase-3-diseno-tecnico/
    │   ├── fase-4-construccion/
    │   ├── fase-5-pruebas-qa/
    │   ├── fase-5/
    │   ├── fase-6-uat/
    │   ├── fase-7-despliegue/
    │   ├── fase-8-estabilizacion/
    │   ├── fase-9-cierre/
    │   ├── fase-R2-correccion/
    │   ├── fase-R2/
    │   └── r2-mcp/
    └── v2/                                  # ← NUEVO — entregables de v2.0.0
        ├── README.md                        # Placeholder: "v2 — En curso"
        ├── fase-0-descubrimiento/
        │   ├── vision-producto.md
        │   ├── epicas-features.md
        │   ├── historias-usuario.md
        │   ├── backlog-priorizado.md
        │   └── presentacion-descubrimiento.html
        ├── fase-1-inicio/
        │   └── ...                          # Conforme avancen las fases
        └── ...
```

### Migración ejecutada

```bash
# 1. Crear carpeta v1
mkdir -p docs/entregables/v1

# 2. Mover fases existentes a v1/
for f in docs/entregables/fase-* docs/entregables/r2-*; do
  mv "$f" docs/entregables/v1/
done

# 3. Crear estructura base v2
mkdir -p docs/entregables/v2/fase-0-descubrimiento
echo "# Abax-Memory v2.0.0 — En curso" > docs/entregables/v2/README.md
```

> **Nota**: Los archivos `docs/bitacora.md` y `docs/registro-entregables.md` permanecen en su ubicación original (`docs/`) como documentos transversales que cubren todo el ciclo de vida del producto, incluyendo ambas iteraciones.

---

## 5. Reglas de Coexistencia v1 / v2

### Principio general

Cada release (`v1`, `v2`, …) es un proyecto autocontenido con su propio ciclo cascada completo (F0 a F9), documentado en su propio folder bajo `docs/entregables/`. Los documentos transversales (`bitacora.md`, `registro-entregables.md`, `iteration-log.md`) residen en `docs/` y referencian ambas releases.

### Reglas de escritura

| Regla | Descripción |
|---|---|
| **R1 — Folder por release** | Todo entregable de v2 se escribe en `docs/entregables/v2/<fase>/<entregable>.md`. Nunca en `docs/entregables/v1/`. |
| **R2 — v1 es solo-lectura** | Los archivos bajo `docs/entregables/v1/` no se modifican. Si se detecta un error factual en v1 que debe corregirse, se documenta en `iteration-log.md` con una entrada de tipo "Corrección retrospectiva v1". |
| **R3 — Documentos transversales** | `docs/bitacora.md`, `docs/registro-entregables.md` e `iteration-log.md` se actualizan con bloques para cada release. Usan formato de secciones por release, no mezclan contenido. |
| **R4 — Release mapping obligatorio** | `docs/release-mapping.md` documenta la relación entre releases: qué cambia (alcance, dominio, audiencia, stack), qué se mantiene, y cómo navegar entre la documentación de v1 y v2. |
| **R5 — Prefijo de release en commits** | Los commits de v2 usan el prefijo `v2:` en el mensaje (ej. `v2: F0 — Visión de Producto`). Los commits que afectan documentos transversales usan `docs:` sin prefijo de release. |
| **R6 — Aprobaciones independientes** | Los gates de fase de v2 son independientes de v1. Que v1 esté cerrado no exime a v2 de pasar por cada gate (F4, F5, F6, F7, F8, F9). |
| **R7 — English-Only en v2** | Todos los identificadores en código, endpoints, modelos, tablas y columnas de v2 deben estar en inglés (skill `code-naming-convention`). Los comentarios y documentación de usuario pueden estar en español. v1 mantiene su idioma original sin cambios retroactivos. |

### Navegación entre releases

```
¿Qué necesito consultar?
├── Estado general del producto → docs/bitacora.md (ambas releases)
├── Registro de entregables → docs/registro-entregables.md (ambas releases)
├── Decisiones de iteración → docs/iteration-log.md (este archivo)
├── Relación v1 ↔ v2 → docs/release-mapping.md
├── Documentación v1 (PMOA IT Ops) → docs/entregables/v1/
└── Documentación v2 (motor multi-dominio) → docs/entregables/v2/
```

### Anti-patrones explícitamente prohibidos

| Anti-patrón | Consecuencia |
|---|---|
| Escribir un entregable v2 en `docs/entregables/fase-X/` (sin prefijo `v2/`) | Se mezcla con v1, rompe la separación por release |
| Modificar un archivo bajo `docs/entregables/v1/` "para que coincida con v2" | Destruye el histórico aprobado de v1 |
| Usar estrategia B (bloque de cambios) para un entregable v2 | Inconsistencia con la estrategia A global |
| No actualizar `iteration-log.md` al iniciar una fase de v2 | Pérdida de trazabilidad de decisiones |
| Mezclar español e inglés en identificadores de v2 | Violación de `code-naming-convention` |

---

## 6. Historial de Decisiones de Iteración

### v2.0.0 — Iniciada 2026-05-03

| Campo | Valor |
|---|---|
| **Versión** | 2.0.0 |
| **Fecha de decisión** | 2026-05-03 |
| **Estrategia** | A — Folder por release |
| **Decidida por** | Usuario (sponsor) |
| **Orquestador** | project-manager |
| **Alcance** | Motor de memoria genérica multi-dominio con perfiles configurables |
| **Stack base** | Mismo que v1 (Quarkus + PostgreSQL + Qdrant + Keycloak + OpenAI) con extensiones para perfiles de dominio |
| **Cambios clave vs v1** | Dominio IT Ops → multi-dominio; audiencia IT → cualquier industria; convención español → English-Only; perfiles de dominio dinámicos |
| **Documentación v1** | Preservada intacta en `docs/entregables/v1/` (42 entregables, 9 fases) |
| **Documentación v2** | Nueva en `docs/entregables/v2/` — Fase 0 en curso |
| **Fase actual v2** | Pre-F0 — Documentación de iteración completada |

---

## Glosario

- **PMOA**: Product Memory for Operations & Analysis — nombre del producto en v1.0.0, una memoria operativa para equipos de IT Operations.
- **GHCR**: GitHub Container Registry — registro de imágenes de contenedor de GitHub donde se publicó la imagen Docker de v1.0.0.
- **Qdrant**: Base de datos vectorial utilizada para búsqueda semántica y almacenamiento de embeddings generados por OpenAI.
- **UAT**: User Acceptance Testing — fase 6 del ciclo cascada donde el Product Owner valida que el producto cumple los criterios de aceptación.
- **RBAC**: Role-Based Access Control — control de acceso basado en roles implementado con Keycloak (OIDC).
- **RTO / RPO**: Recovery Time Objective (tiempo máximo para recuperar el servicio) / Recovery Point Objective (cantidad máxima de datos que se tolera perder).
