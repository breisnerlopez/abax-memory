# Diagnóstico: Problema del OIDC Híbrido en Abax-Memory v2.0.0

- **Fase**: construction (v1.0.0 — archivado desde `docs/entregables/fase-construction/` el 2026-05-07)
- **Responsable**: Tech Lead
- **Fecha**: 2026-05-04
- **Estado**: Completado — Archivado en v1 (corrección retrospectiva de organización de carpetas)

---

## 1. ¿Qué hace exactamente el TenantFilter hoy?

`TenantFilter` es un filtro HTTP que intercepta **toda petición a `/api/v2/*`** y extrae el identificador del tenant (cliente/organización). Tiene dos caminos:

| Camino | Condición | Fuente del tenant | Uso |
|--------|-----------|-------------------|-----|
| **OIDC (producción)** | `quarkus.oidc.enabled=true` + usuario autenticado | Claim `tenant_id` o `azp` del JWT emitido por Keycloak | Producción |
| **Header (dev/test)** | OIDC deshabilitado o sin claim | Header HTTP `X-Tenant-Id` | Desarrollo local |

Si ningún camino produce un tenant, el filtro **aborta la petición con HTTP 401**.

El filtro delega la extracción real del tenant a `TenantContext`, que es un bean `@RequestScoped` — una instancia por petición HTTP.

---

## 2. ¿Por qué usa reflexión? ¿Qué intenta acceder y por qué no puede hacerlo directamente?

La reflexión está en `TenantContext.extractClaimFromPrincipal()` (líneas 168–193). El código hace esto:

```java
var getClaimMethod = principal.getClass().getMethod("getClaim", String.class);
Object claimValue = getClaimMethod.invoke(principal, claimName);
```

### ¿Qué intenta acceder?

Quiere llamar al método `getClaim(String)` del objeto `Principal` que Quarkus/OIDC inyecta. Este método existe en clases internas como:

- `io.quarkus.oidc.runtime.OidcJwtCallerPrincipal`
- `io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal`

### ¿Por qué no puede hacerlo directamente?

**Porque el proyecto tiene DOS extensiones de seguridad JWT funcionando al mismo tiempo**, y el código no sabe cuál de las dos clases de `Principal` va a recibir en tiempo de ejecución:

- `quarkus-oidc` → produce `OidcJwtCallerPrincipal`
- `quarkus-smallrye-jwt` → produce `DefaultJWTCallerPrincipal`

El código ya intenta el camino limpio **primero** — usando la interfaz estándar `JsonWebToken` de MicroProfile JWT (líneas 127–144). Pero cuando ese camino falla (porque las dos extensiones compiten y `JsonWebToken` no se resuelve correctamente), cae en la reflexión como **plan B desesperado** para invocar `getClaim()` sin importar qué tipo concreto de Principal haya.

### El problema de fondo: dos extensiones que se pisan

`TenantContext` se inyecta **ambas** cosas:

```java
@Inject Instance<SecurityIdentity> securityIdentityInstance;  // ¿Quién lo puebla?
@Inject Instance<JsonWebToken> jwtInstance;                   // ¿Quién lo puebla?
```

Con `quarkus-oidc` Y `quarkus-smallrye-jwt` en el classpath, ambas extensiones intentan procesar el token JWT entrante. El resultado es **no determinístico**: a veces `JsonWebToken` se resuelve, a veces no. La reflexión existe como muleta para cuando no se resuelve.

---

## 3. ¿Está quarkus-oidc en el pom.xml?

**Sí.** Está declarado explícitamente:

```xml
<!-- pom.xml, líneas 73-76 -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-oidc</artifactId>
</dependency>
```

**Pero también está `quarkus-smallrye-jwt`** (líneas 77–80):

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-jwt</artifactId>
</dependency>
```

Y en `application.properties` hay configuración de **ambos** mundos:

```properties
# Configuración OIDC (líneas 10-14)
quarkus.oidc.application-type=service
quarkus.oidc.auth-server-url=...
quarkus.oidc.client-id=...

# Configuración MP-JWT (líneas 15-16) ← ¡Sobra!
mp.jwt.verify.issuer=...
mp.jwt.verify.audiences=...
```

**Ese es el híbrido**: dos mecanismos de autenticación JWT compitiendo.

---

## 4. ¿Qué falta para que funcione con JWT real de Keycloak sin reflexión?

Muy poco. La solución es **eliminar la duplicidad**. Tres pasos concretos:

### Paso 1 — Quitar `quarkus-smallrye-jwt` del pom.xml

`quarkus-oidc` **ya incluye** el soporte para `JsonWebToken` de MicroProfile. No necesitas ambas. Quitar la dependencia:

```xml
<!-- ELIMINAR este bloque -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-jwt</artifactId>
</dependency>
```

### Paso 2 — Quitar las propiedades `mp.jwt.verify.*`

En `application.properties`, eliminar las líneas 15–16:

```properties
# ELIMINAR estas líneas
mp.jwt.verify.issuer=${MP_JWT_VERIFY_ISSUER:http://localhost:8443/realms/abax-memory}
mp.jwt.verify.audiences=${MP_JWT_VERIFY_AUDIENCES:abax-memory-api}
```

### Paso 3 — Simplificar `TenantContext.resolveFromJwt()`

Con solo `quarkus-oidc`, el `JsonWebToken` inyectable **siempre** se resolverá correctamente. Se puede eliminar todo el bloque de reflexión (`extractClaimFromPrincipal`, líneas 168–193) y la segunda mitad del método `resolveFromJwt()` (líneas 146–154). El camino limpio por `JsonWebToken` será suficiente.

Adicionalmente, quitar el import fantasma en `TenantFilter.java` línea 3:

```java
import io.quarkus.oidc.runtime.OidcConfig;  // ← no se usa en ninguna parte
```

### ¿Hay que tocar Keycloak?

**No.** El realm, los clientes, los claims `tenant_id` y `azp` ya existen (o deben configurarse en Keycloak, pero eso es configuración del IdP, no del código). El problema está 100% del lado de Quarkus.

---

## 5. ¿Es un problema de configuración, de dependencia faltante, o de diseño?

### Es un problema de **diseño** — concretamente, de **decisión arquitectónica no resuelta**.

| Tipo de problema | ¿Aplica? | Evidencia |
|------------------|----------|-----------|
| Configuración | Parcialmente | Las props `mp.jwt.verify.*` no deberían coexistir con `quarkus.oidc.*` |
| Dependencia faltante | **No** | `quarkus-oidc` está presente. El problema es una dependencia **de más** (`quarkus-smallrye-jwt`) |
| **Diseño** | **Sí** | Tener dos extensiones JWT simultáneas + reflexión como escape hatch es una decisión de diseño que nunca se resolvió correctamente |

La evidencia de que es diseño y no un simple error de configuración:

1. El código **sabe** que hay dos caminos: primero intenta `JsonWebToken` (MP-JWT), luego cae en reflexión.
2. El import sin usar `io.quarkus.oidc.runtime.OidcConfig` sugiere que alguien intentó acceder a la config interna de OIDC y luego abandonó ese camino.
3. Los comentarios del código son muy explícitos sobre el comportamiento dual (OIDC vs header), pero **nunca mencionan por qué hay dos extensiones JWT**. Eso indica que nadie tomó la decisión consciente de elegir UNA.
4. La configuración tiene ambas familias de propiedades (`quarkus.oidc.*` y `mp.jwt.*`) como si fueran complementarias, cuando en realidad son mutuamente excluyentes.

---

## Solución propuesta (la más simple)

### Principio: usar UN solo mecanismo de autenticación JWT

**Quarkus OIDC** (`quarkus-oidc`) es la opción correcta porque:
- Está diseñado para integrarse con Keycloak (el IdP del proyecto)
- Soporta `JsonWebToken` de MicroProfile de forma nativa (sin necesidad de `quarkus-smallrye-jwt`)
- Maneja service-to-service (client credentials) que es el patrón `application-type=service`
- Es la extensión recomendada por Quarkus para OIDC/OAuth2

### Acciones concretas

| # | Archivo | Acción | Riesgo |
|---|---------|--------|--------|
| 1 | `pom.xml` | Eliminar dependencia `quarkus-smallrye-jwt` | Bajo — OIDC cubre el mismo caso de uso |
| 2 | `application.properties` | Eliminar líneas `mp.jwt.verify.*` | Bajo — eran redundantes |
| 3 | `TenantFilter.java` | Eliminar `import io.quarkus.oidc.runtime.OidcConfig` (línea 3) | Nulo — no se usa |
| 4 | `TenantContext.java` | Eliminar método `extractClaimFromPrincipal()` (líneas 168–193) | Bajo — el camino por `JsonWebToken` cubre el mismo caso |
| 5 | `TenantContext.java` | Eliminar bloque de reflexión en `resolveFromJwt()` (líneas 146–154) | Bajo — idem |
| 6 | Tests | Verificar que `@TestSecurity(user=..., roles=...)` + `quarkus-test-security` sigue funcionando | Medio — toca verificar |

### Estimación: 2–4 horas

- 30 min para los cambios de código
- 1–2 horas para verificar que los tests existentes pasan
- 30 min para prueba manual con Keycloak real (o simulado con `quarkus-test-security`)

### Lo que NO hay que tocar

- **NO** eliminar `quarkus-oidc` — es la extensión correcta
- **NO** cambiar la lógica de fallback a `X-Tenant-Id` header — eso es un feature válido para desarrollo local
- **NO** tocar Keycloak — el problema no está en el IdP
- **NO** modificar `TenantFilter` más allá de quitar el import fantasma — su lógica de dos caminos (JWT vs header) es correcta

---

## Diagrama: antes vs después

### Antes (híbrido roto)

```
Petición con JWT
    │
    ▼
┌──────────────────────────────────────┐
│  quarkus-oidc  │  quarkus-smallrye-jwt │  ← DOS extensiones
│  procesa JWT   │  procesa JWT          │     compitiendo
└───────┬────────┴────────┬─────────────┘
        │                 │
        ▼                 ▼
  OidcJwtCallerPrincipal  DefaultJWTCallerPrincipal
        │                 │
        └────────┬────────┘
                 ▼
     TenantContext.resolveFromJwt()
        │
        ├── JsonWebToken? → A VECES funciona
        │
        └── Fallback: REFLEXIÓN sobre Principal
            getMethod("getClaim").invoke(...)  ← MUERTA técnica
```

### Después (limpio, sin reflexión)

```
Petición con JWT
    │
    ▼
┌─────────────────┐
│  quarkus-oidc   │  ← UNA extensión
│  procesa JWT    │
└────────┬────────┘
         │
         ▼
  OidcJwtCallerPrincipal
  (implementa JsonWebToken)
         │
         ▼
  TenantContext.resolveFromJwt()
         │
         └── jwtInstance.get().getClaim("tenant_id")  ← Funciona SIEMPRE
```

---

## Glosario

- **OIDC (OpenID Connect)**: Protocolo de autenticación sobre OAuth2 que emite JWTs con claims de identidad. Keycloak es un proveedor OIDC.
- **JWT (JSON Web Token)**: Token con claims (datos) firmados digitalmente. El estándar para transportar identidad entre servicios.
- **Claim**: Un par clave-valor dentro del JWT. Ej: `tenant_id`, `azp` (authorized party), `sub` (subject).
- **SmallRye JWT / MP-JWT**: Implementación del estándar MicroProfile JWT. `quarkus-smallrye-jwt` es la extensión que lo integra. Es redundante si ya usas `quarkus-oidc`.
- **Principal**: En Java/seguridad, el objeto que representa al usuario autenticado. Puede tener claims del JWT.
- **CDI (Contexts and Dependency Injection)**: El sistema de inyección de dependencias de Quarkus/Jakarta EE.
