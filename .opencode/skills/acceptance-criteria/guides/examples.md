# examples

## Ejemplo: Login de usuario
**Given** un usuario registrado con credenciales validas
**When** ingresa su email y contrasena y presiona "Ingresar"
**Then** el sistema lo redirige al dashboard principal

**Given** un usuario con contrasena incorrecta
**When** ingresa email valido y contrasena incorrecta
**Then** el sistema muestra "Credenciales invalidas" y no permite acceso
