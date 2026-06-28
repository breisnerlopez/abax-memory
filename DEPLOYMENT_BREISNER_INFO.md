# Despliegue en `demo.breisner.info/memoria`

Despliegue operativo para el servidor dev remoto `breisner.info`, alineado con la cadena `Cloudflare -> cloudflared -> Traefik -> app`.

## Alcance

- URL publica: `https://demo.breisner.info/memoria/`
- Tipo: app publica sin autenticacion
- Entrada Traefik: `demo-memoria.yml` en `traefik/dynamic/`
- Stack Docker: `docker-compose.prod.yml`
- Backend: build local desde esta copia del repo (`deploy/backend.Dockerfile`)

## Decisiones operativas

- Se publica el frontend React por Traefik y se hace proxy interno a `/api/` y `/q/` hacia el backend Quarkus.
- No se exponen `ports:` al host.
- Se omite Keycloak en este despliegue y no se aplica Authentik porque la publicacion es deliberadamente publica bajo `demo.breisner.info/memoria/`.
- La imagen GHCR `ghcr.io/breisnerlopez/abax-memory:latest` no fue suficiente para este servidor: exigia configuracion OIDC y resolucion de OpenAI no compatibles con este despliegue. Por eso el backend se construye localmente desde source con OIDC removido del artefacto de despliegue.
- El frontend se compila con base `/memoria/` para que SPA, assets y llamadas API funcionen bajo subruta.
- PostgreSQL y Qdrant quedan dedicados a esta app en la red privada `abax-memory`.

## Preparacion de secretos

```bash
cp /workspace/ops/abax-memory/.env.prod.example /workspace/ops/abax-memory/.env.prod
chmod 600 /workspace/ops/abax-memory/.env.prod
```

Completar como minimo:

- `OPENAI_API_KEY`
- `ABAX_POSTGRES_PASSWORD`

## Despliegue

```bash
docker network create secure-publishing || true
docker compose --env-file /workspace/ops/abax-memory/.env.prod -f /workspace/ops/abax-memory/docker-compose.prod.yml up -d --build
docker compose --env-file /workspace/ops/abax-memory/.env.prod -f /workspace/ops/abax-memory/docker-compose.prod.yml ps
```

## Traefik

Copiar el router a la infra real del servidor si el file provider activo vive en `/opt`:

```bash
cp /workspace/ops/infra/secure-publishing/traefik/dynamic/demo-memoria.yml /opt/secure-publishing/traefik/dynamic/demo-memoria.yml
docker compose -f /opt/secure-publishing/docker-compose.yml up -d traefik
```

## Cloudflare Tunnel

No requiere hostname nuevo si `demo.breisner.info` ya esta publicado al Traefik. Solo se agrega la subruta en Traefik.

## Validacion

```bash
docker compose --env-file /workspace/ops/abax-memory/.env.prod -f /workspace/ops/abax-memory/docker-compose.prod.yml ps
curl -I -H 'Host: demo.breisner.info' http://127.0.0.1:8081/memoria/
curl http://127.0.0.1:8081/memoria/q/health -H 'Host: demo.breisner.info'
```

Esperado:

- `curl -I` devuelve `200` o `304`, sin redirigir a login
- `frontend`, `backend`, `postgres`, `qdrant` en estado `Up`
- `https://demo.breisner.info/memoria/` carga el frontend directamente
