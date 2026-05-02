# dockerfile-optimizado

## Ejemplo de multi-stage build (Node.js)

```dockerfile
# Etapa de build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force
COPY . .
RUN npm run build

# Etapa de ejecucion
FROM node:20-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
USER appuser
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:3000/health || exit 1
CMD ["node", "dist/main.js"]
```

## Puntos clave

- La imagen final no contiene el codigo fuente ni herramientas de build.
- El usuario no es root.
- El HEALTHCHECK permite al orquestador detectar fallos.
- Se usa npm ci (instalacion limpia) en lugar de npm install.
