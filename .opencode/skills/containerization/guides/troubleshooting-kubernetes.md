# troubleshooting-kubernetes

## Comandos de diagnostico esenciales

- `kubectl get pods -o wide`: Ver estado de pods con nodo asignado.
- `kubectl describe pod <nombre>`: Detalles del pod, eventos y errores.
- `kubectl logs <pod> -c <container> --previous`: Logs del contenedor
  anterior (util si el pod reinicio).
- `kubectl exec -it <pod> -- /bin/sh`: Acceder al contenedor en ejecucion.
- `kubectl top pods`: Ver consumo de CPU y memoria.
- `kubectl get events --sort-by=.metadata.creationTimestamp`: Eventos
  recientes del cluster.

## Problemas comunes

- **CrashLoopBackOff**: La aplicacion falla al iniciar. Revisar logs
  y verificar configuracion/secrets.
- **ImagePullBackOff**: No se puede descargar la imagen. Verificar
  nombre, tag y credenciales del registry.
- **Pending**: No hay nodos con recursos suficientes. Revisar requests
  y la capacidad del cluster.
- **OOMKilled**: El contenedor excedio el limite de memoria. Aumentar
  limits o investigar memory leaks.
- **Readiness probe failed**: El endpoint de health no responde.
  Verificar que la aplicacion inicie correctamente.
