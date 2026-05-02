# configuracion-por-ambiente

## Estrategias de configuracion

### Variables de entorno
- Metodo mas simple y universal.
- Usar archivos .env por ambiente (nunca commitear .env.prod).
- En Kubernetes: ConfigMaps para configuracion, Secrets para datos sensibles.

### Servidores de configuracion
- Spring Cloud Config, Consul, etcd para configuracion centralizada.
- Permiten cambiar configuracion sin redesplegar.
- Implementar versionado y auditoria de cambios.

### Feature flags
- Desacoplar el despliegue de la activacion de funcionalidades.
- Herramientas: LaunchDarkly, Unleash, Flagsmith.
- Permiten activar features por ambiente, usuario o porcentaje.

## Que debe variar entre ambientes

- URLs de APIs y servicios externos.
- Credenciales de bases de datos y servicios.
- Niveles de logging (DEBUG en dev, WARN/ERROR en prod).
- Configuracion de cache y timeouts.
- Certificados TLS y dominios.
- Feature flags y configuracion de A/B testing.

## Que NO debe variar entre ambientes

- Codigo fuente compilado (artefactos binarios).
- Versiones de dependencias.
- Esquema de base de datos (aplicar migraciones en todos los ambientes).
- Logica de negocio.
