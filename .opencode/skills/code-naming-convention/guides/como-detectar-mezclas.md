# como-detectar-mezclas

## Como detectar mezclas en un codebase existente

Patron de busqueda regex (ajustar al stack):

```bash
# TypeScript/JavaScript: identificadores camelCase con palabras espanolas comunes
grep -rEn 'cantidad|fecha|usuario|cliente|pedido|factura|articulo|producto|orden|nombre|apellido|correo|telefono|direccion|monto|valor|moneda|precio|cantidad|calcular|obtener|listar|crear|borrar|eliminar|guardar|actualizar|verificar|validar|enviar|recibir|consultar|notificar|iniciar|terminar|registrar' src/ \
  --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' \
  | grep -vE '//.*|/\*.*|\*/|".*"|`.*`'

# Endpoints en espanol
grep -rEn '/(usuarios|pedidos|clientes|facturas|productos|articulos|ordenes|categorias|catalogos)' src/ data/

# Tablas SQL en espanol (en migrations/schemas)
grep -rEn 'CREATE TABLE (usuarios|pedidos|clientes|facturas|productos|articulos|ordenes)' migrations/ schema/ db/

# Env vars en espanol
grep -rEn '^(URL_|CLAVE_|TOKEN_|SECRETO_|CONFIGURACION_|PARAMETRO_)[A-Z_]+' .env* deployment/ k8s/
```

Reportar al orquestador en formato:

```markdown
## Inventario de identificadores en espanol

| Archivo | Linea | Identificador | Sugerencia |
|---|---|---|---|
| src/services/user.ts | 23 | `obtenerCliente` | `getCustomer` |
| src/api/orders.ts | 45 | `/api/pedidos` | `/api/orders` |
| migrations/001.sql | 3 | `CREATE TABLE pedidos` | `orders` |

Estimacion de impacto: <X archivos>, <Y lineas>, <Z dias de migracion>.
```

Recordar: NO modificar tablas/APIs publicas legacy sin plan de migracion
explicito y aprobado por sponsor.
