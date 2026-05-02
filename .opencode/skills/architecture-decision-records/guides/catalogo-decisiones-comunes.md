# catalogo-decisiones-comunes

## Catalogo de Decisiones Arquitectonicas Frecuentes

### Infraestructura y despliegue
- Eleccion de proveedor cloud (AWS, Azure, GCP).
- Estrategia de contenedores vs serverless vs VMs.
- Modelo de despliegue (multi-region, multi-AZ, single-region).

### Comunicacion entre servicios
- Sincrona (REST, gRPC) vs asincrona (eventos, colas).
- Eleccion de message broker (RabbitMQ, Kafka, SQS).
- Estrategia de API Gateway y service mesh.

### Persistencia de datos
- Tipo de base de datos (relacional, documental, grafos, time-series).
- Estrategia de cache (Redis, Memcached, cache local).
- Patron de acceso a datos (CQRS, Event Sourcing, CRUD).

### Seguridad y autenticacion
- Proveedor de identidad (Keycloak, Auth0, Azure AD).
- Estrategia de autorizacion (RBAC, ABAC, policy-based).
- Gestion de secretos (Vault, AWS Secrets Manager, Azure Key Vault).
