# gobernanza-y-acceso

## Control de acceso por ambiente

| Ambiente   | Desarrollo | QA         | Staging    | Produccion |
|------------|-----------|------------|------------|------------|
| Developers | Full      | Read+Deploy| Read       | Read logs  |
| QA         | Read      | Full       | Read+Test  | Read logs  |
| DevOps     | Full      | Full       | Full       | Full       |
| Product    | None      | Read       | Read+Test  | Read       |

## Politicas de gobernanza

- Todo cambio en produccion debe tener un ticket asociado y aprobacion.
- Los despliegues a produccion deben registrar: quien, que, cuando.
- Los accesos directos a bases de datos de produccion requieren
  autorizacion temporal (just-in-time access).
- Realizar auditorias periodicas de permisos y accesos.
- Implementar break-glass procedures para emergencias documentadas.
