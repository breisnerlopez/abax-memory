# checklist-pre-go-live

Checklist que el devops ejecuta el dia del go-live, en orden:

1. Plan de despliegue aprobado por sponsor — verificar firma en `00-plan-despliegue.md`.
2. Rollback probado en staging en las ultimas 48h — verificar fecha en plan.
3. Monitoring activo — verificar dashboard responde.
4. Comunicacion previa enviada — verificar timestamp.
5. Equipo oncall identificado y disponible — verificar contactos.
6. Backup reciente disponible (si aplica BD) — verificar timestamp y restore-test.
7. DNS / certificados validos — `curl -I https://<dominio>` desde fuera de la red.
8. Ejecutar deploy.
9. Smoke test post-deploy.
10. Actualizar pagina de status si aplica.

Si alguno falla, abort y rollback inmediato.
