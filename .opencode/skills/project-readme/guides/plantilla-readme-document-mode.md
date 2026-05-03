# plantilla-readme-document-mode

## Plantilla README — modo document (sistema existente)

El README documenta lo que el sistema ES y como esta operado hoy.
NO promete reescritura ni propone modernizacion (eso va separado).

```markdown
# <NombreSistema> — Documentacion

> **Inventario tecnico, funcional y operativo de `<NombreSistema>`,
> generado como parte del esfuerzo de documentacion <fecha>.**

Este README es el indice de la documentacion del sistema. El sistema
en si vive en `<ruta-real>` o en `<servidor>`.

## Que es `<NombreSistema>`

- **Tipo**: <ej. aplicacion web monolitica PHP 5.6 / desktop Java Swing / mainframe Cobol>
- **Proposito**: <una frase>
- **Usuarios**: <quien lo usa, cuantos, donde>
- **Estado**: produccion desde <ano>, mantenido por <equipo>.
- **Stack**: <ej. PHP 5.6 + MySQL 5.7 + Apache 2.4 en CentOS 7>

## Como CORRER el sistema (no como modernizarlo)

```bash
# Procedimiento real, validado contra el sistema actual:
ssh user@servidor.interno
cd /var/www/sistema
sudo systemctl status apache2
# ...
```

Si requiere acceso a infra restringida, indicarlo:

> Acceso al servidor requiere VPN corporativa + credencial RDP /
> usuario LDAP. Solicitar a `<equipo>`.

## Documentacion completa

| Aspecto | Documento |
|---|---|
| Arquitectura tecnica actual | [docs/arquitectura.md](docs/arquitectura.md) |
| Modelo de datos | [docs/modelo-datos.md](docs/modelo-datos.md) |
| Endpoints / interfaces | [docs/api.md](docs/api.md) |
| Reglas de negocio | [docs/reglas-negocio.md](docs/reglas-negocio.md) |
| Procedimientos operativos (deploy, backup, restore) | [docs/runbook.md](docs/runbook.md) |
| Manuales de usuario | [docs/manuales/](docs/manuales/) |
| Glosario de terminos del dominio | [docs/glosario.md](docs/glosario.md) |
| Recomendaciones de modernizacion (separado) | [docs/recommendations.md](docs/recommendations.md) |

## Como se genero esta documentacion

Producida por el equipo de documentacion <fecha> usando Abax Swarm
en modo `document`. Las afirmaciones sobre el sistema fueron validadas
contra el codigo y la infraestructura real (referencias `archivo:linea`
en cada documento).

## Mantenimiento de esta documentacion

Cada cambio significativo en el sistema debe reflejarse en los docs.
Owner: <equipo / persona>. Revision trimestral programada.

## Licencia / propiedad

<Indicar politica: codigo propietario uso interno, MIT, etc.>
```
