# Diagnóstico: Presentaciones no visibles en GitHub Pages
- **Fase**: 9 - Cierre
- **Responsable**: DevOps Engineer
- **Fecha**: 2026-05-02
- **Estado**: Resuelto
---

## 1. Síntoma reportado

El usuario accede a `https://breisnerlopez.github.io/abax-memory/` y no ve las presentaciones del proyecto.

## 2. Diagnóstico inicial

### Verificación HTTP

| URL | HTTP | Diagnóstico |
|-----|------|-------------|
| `/` | 200 | ❌ Servía README renderizado por **Jekyll** |
| `/index.html` | 200 | ❌ Mismo contenido Jekyll |
| `/docs/index.html` | 200 | ✅ Nuestro índice existía pero en ruta incorrecta |
| `/entregables/fase-0-descubrimiento/...` | 404 | ❌ |
| `/docs/entregables/fase-0-descubrimiento/...` | 200 | ✅ Archivos existían pero en ruta incorrecta |

### Causa raíz identificada

GitHub Pages estaba procesando el repositorio con **Jekyll** (comportamiento por defecto). Jekyll tomaba `README.md` de la raíz y lo convertía en la página principal (`/index.html`), **sombreando** completamente el contenido de `docs/index.html`.

Aunque GitHub Pages estaba configurado para servir desde la carpeta `docs/`, Jekyll generaba un sitio estático desde la raíz del repo que tomaba precedencia sobre los archivos estáticos en `docs/`.

**Evidencia**: El HTML servido en `/` contenía metadatos de Jekyll:
```html
<meta name="generator" content="Jekyll v3.10.0" />
```

## 3. Solución aplicada

### Archivo `.nojekyll` en raíz del repositorio

Se creó un archivo vacío `.nojekyll` en la raíz del repositorio. Esto le indica a GitHub Pages que **no** procese el repositorio con Jekyll, sirviendo los archivos estáticos tal cual desde la carpeta configurada (`docs/`).

```
commit dcbe5aa: fix: corregir rutas y agregar .nojekyll para GitHub Pages
commit 0ce4c1a: fix: revertir rutas docs/ (la solución real es .nojekyll)
```

### Lo que NO era el problema

Inicialmente se sospechó que los enlaces en `docs/index.html` necesitaban el prefijo `docs/`. Esto resultó ser **incorrecto**. Una vez que Jekyll fue desactivado, GitHub Pages sirve correctamente desde `docs/` como raíz, por lo que las rutas relativas `entregables/...` son las correctas.

## 4. Verificación post-corrección

| URL | HTTP | Contenido |
|-----|------|-----------|
| `/` | 200 | ✅ Índice de presentaciones (Design System PMOA) |
| `/entregables/fase-0-descubrimiento/presentacion-descubrimiento.html` | 200 | ✅ |
| `/entregables/fase-1-inicio/presentacion-kickoff.html` | 200 | ✅ |
| `/entregables/fase-2-analisis/presentacion-propuesta-funcional.html` | 200 | ✅ |
| `/entregables/fase-3-diseno-tecnico/presentacion-arquitectura.html` | 200 | ✅ |
| `/entregables/fase-4-construccion/presentacion-avance.html` | 200 | ✅ |
| `/entregables/fase-6-uat/presentacion-resultados-uat.html` | 200 | ✅ |
| `/entregables/fase-7-despliegue/presentacion-go-live.html` | 200 | ✅ |
| `/entregables/fase-9-cierre/presentacion-cierre.html` | 200 | ✅ |

**9 de 9 URLs verificadas exitosamente.**

## 5. Lección aprendida

- Si GitHub Pages sirve desde `docs/` pero muestra contenido Jekyll (README), **no** es un problema de rutas: es Jekyll tomando control del sitio.
- La solución es siempre agregar `.nojekyll` en la raíz del repositorio cuando se quiere servir HTML estático desde `docs/`.
- Verificar el meta tag `<meta name="generator" content="Jekyll...">` en el HTML servido es la forma más rápida de diagnosticar este problema.
