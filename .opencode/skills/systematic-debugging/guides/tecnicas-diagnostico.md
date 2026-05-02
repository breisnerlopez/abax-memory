# tecnicas-diagnostico

## Guia de Tecnicas de Diagnostico

### 1. Biseccion con Git (git bisect)
Util cuando sabes que algo funciono antes y dejo de funcionar.

```bash
git bisect start
git bisect bad              # commit actual (roto)
git bisect good <commit>    # ultimo commit conocido donde funcionaba
# Git te presenta commits intermedios, marcas cada uno como good/bad
# hasta encontrar el commit exacto que introdujo el bug
git bisect reset            # cuando termines
```

### 2. Logging Estrategico Temporal
Agregar logs en puntos clave para entender el flujo en runtime.

- Loguear entradas y salidas de funciones sospechosas.
- Incluir valores de variables criticas.
- Usar niveles de log apropiados (DEBUG para diagnostico temporal).
- Marcar los logs temporales con un prefijo como `[DEBUG-TEMP]` para
  facilitar su eliminacion posterior.
- NUNCA dejar logs de diagnostico temporal en el codigo final.

### 3. Debugger Interactivo
Cuando necesitas inspeccionar el estado del programa en un punto especifico.

- Colocar breakpoints en las lineas sospechosas.
- Inspeccionar variables locales y estado del objeto.
- Usar step-over para avanzar linea por linea.
- Usar step-into para entrar en funciones llamadas.
- Evaluar expresiones en el contexto del breakpoint.

### 4. Reduccion del Caso de Prueba
Simplificar el escenario hasta tener el caso minimo que reproduce el bug.

- Eliminar datos innecesarios del input.
- Eliminar pasos intermedios que no afectan la reproduccion.
- Aislar el componente: probar sin dependencias externas si es posible.
- El caso reducido es mas facil de analizar y se convierte en el test.

### 5. Analisis de Diferencias (Diff)
Comparar el estado que funciona con el estado roto.

- Comparar configuraciones entre entornos.
- Comparar versiones de dependencias.
- Comparar datos de entrada en caso exitoso vs caso fallido.
- Buscar la diferencia minima que causa el cambio de comportamiento.
