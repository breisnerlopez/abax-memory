# prevencion-regresion

## Guia de Prevencion de Regresiones

### Que es una Regresion
Una regresion es un defecto introducido por un cambio que rompe funcionalidad
que antes funcionaba correctamente. Es uno de los tipos de bug mas frustrantes
y evitables.

### Estrategia de Tests de Regresion

1. **Por cada bug corregido, un test permanente:**
   Cada bug que se corrige debe tener al menos un test automatizado que
   lo reproduzca. Este test se agrega a la suite permanente y nunca se elimina.

2. **Nombrar tests de regresion de forma descriptiva:**
   El nombre del test debe indicar que escenario cubre y referenciar el ticket.
   Ejemplo: `test_user_login_fails_gracefully_when_email_is_null_BUG_1234`

3. **Cubrir el caso exacto y las variantes:**
   No solo el caso reportado, sino variantes cercanas que podrian tener
   el mismo problema (ej: null, vacio, espacios en blanco).

4. **Ejecutar tests de regresion en cada build:**
   Los tests de regresion deben ser parte de la suite que se ejecuta
   en cada CI build, no en una suite separada que se corre esporadicamente.

### Patrones Comunes de Regresion y Como Prevenirlos

| Patron                        | Prevencion                                          |
|-------------------------------|-----------------------------------------------------|
| Cambio en funcion compartida  | Tests de integracion para todos los consumidores.   |
| Actualizacion de dependencia  | Tests end-to-end y lock de versiones.               |
| Cambio de configuracion       | Tests que validan configuracion en cada entorno.    |
| Refactorizacion               | Suite completa de tests antes de refactorizar.      |
| Merge conflictivo             | Ejecutar tests completos despues de cada merge.     |
