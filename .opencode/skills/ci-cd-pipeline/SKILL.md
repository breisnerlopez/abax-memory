---
name: ci-cd-pipeline
description: Diseno e implementacion de pipelines de integracion continua y despliegue continuo para automatizar build, test y deploy.

---

# Pipeline CI/CD

## Etapas estandar del pipeline
1. **Checkout**: clonar codigo fuente.
2. **Install**: instalar dependencias.
3. **Lint**: validar estilo y reglas estaticas.
4. **Test**: ejecutar tests unitarios.
5. **Build**: compilar/empaquetar artefacto.
6. **Scan**: analisis de seguridad (SAST/SCA).
7. **Docker**: construir imagen de contenedor.
8. **Push**: subir imagen a registry.
9. **Deploy**: desplegar a ambiente target.
10. **Smoke**: ejecutar smoke tests post-deploy.

## Principios
- Fail fast: lint y tests unitarios primero.
- Cache de dependencias para acelerar builds.
- Secretos nunca en codigo, usar secrets del CI.
- Ambientes separados: dev -> qa -> staging -> prod.
- Rollback automatico si smoke test falla.

## Cuando usar esta habilidad
- Al configurar un proyecto nuevo.
- Al agregar etapas al pipeline existente.
- Al optimizar tiempos de build/deploy.

## github-actions-template
## Ejemplo GitHub Actions basico
```yaml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
      - run: npm ci
      - run: npm run lint
      - run: npm test
      - run: npm run build
```
