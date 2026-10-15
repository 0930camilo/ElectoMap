# Gestión de campaña electoral - backend

API REST construida con Java 17, Spring Boot 3, Spring Security, JWT, Spring Data JPA, Flyway y MySQL.

Las relaciones organizativas son: un administrador puede tener varias cabeceras; cada cabecera pertenece a un solo administrador; cada cabecera puede tener varios líderes y cada líder pertenece a una sola cabecera. Los endpoints administrativos de cabeceras y líderes limitan la consulta y gestión a las cabeceras propiedad del administrador autenticado. Los votantes, usuarios de su organización y registros de auditoría también se filtran por el administrador autenticado; las transferencias de votantes no cruzan organizaciones.

La migración `V4` asigna las cabeceras preexistentes al administrador de menor ID, ya que el esquema anterior no guardaba propietario. Si la base contiene varios administradores y las cabeceras históricas tienen dueños distintos, revisa/corrige esos propietarios antes de operar con ellos.

## Requisitos

- JDK 17 o posterior.
- MySQL 8.
- PowerShell (o una terminal compatible con Maven Wrapper).

## Configuración local

1. Crear la base de datos antes de iniciar la aplicación:

   ```sql
   CREATE DATABASE Electo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Configurar estas variables de entorno. `JWT_SECRET` debe contener al menos 32 bytes; use un valor aleatorio propio y no lo incluya en el repositorio.

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/Electo?serverTimezone=UTC"
   $env:DB_USER = "root"
   $env:DB_PASSWORD = "<contraseña-local>"
   $env:JWT_SECRET = "<secreto-aleatorio-de-al-menos-32-bytes>"
   ```

   También se pueden configurar `PORT` (por defecto `8080`), `JWT_EXPIRATION_MS` (por defecto un día) y `CORS_ALLOWED_ORIGINS` (por defecto `http://localhost:4200`). Flyway aplica automáticamente las migraciones de `src/main/resources/db/migration`; Hibernate valida el esquema y no lo crea ni modifica.

3. Iniciar:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   La migración de datos de desarrollo crea `admin` y `lider.prueba`, ambas con contraseña inicial `Cambiar123!`, además de una cabecera y un líder de prueba. Cambie o desactive esas cuentas antes de usar datos reales; no reutilice estas credenciales en producción.

## Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas usan H2 en memoria y no requieren MySQL. Para el empaquetado:

```powershell
.\mvnw.cmd package
```

## API

- Documentación interactiva: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/api-docs`
- Las respuestas usan `{ "success": true, "message": "...", "data": ... }`. Las listas paginadas incluyen contenido y metadatos de página.
- Iniciar sesión con `POST /api/auth/login`; enviar el token como `Authorization: Bearer <token>`.

| Recurso | Rutas principales | Acceso |
| --- | --- | --- |
| Sesión | `POST /api/auth/login` | Público |
| Cabeceras | `GET/POST /api/cabeceras`, `GET/PUT /api/cabeceras/{id}`, `PATCH /api/cabeceras/{id}/estado` | Consulta autenticada; administración `ADMIN` |
| Líderes | `GET/POST /api/lideres`, `GET/PUT /api/lideres/{id}`, `PATCH /api/lideres/{id}/estado`, `GET /api/lideres/me` | Gestión `ADMIN`; perfil propio `LIDER` |
| Posibles votantes | `GET/POST /api/votantes`, `POST /api/votantes/administrador`, `GET /api/votantes/{id}`, `GET /api/votantes/documento/{documento}`, `PUT /api/votantes/{id}`, `PATCH /api/votantes/{id}/estado` | Consulta autenticada; registro `LIDER` o `ADMIN` (para `ADMIN`, asigna por documento del líder) |
| Asignaciones e importación | `PUT /api/votantes/{id}/lider`, `GET /api/votantes/{id}/historial-asignaciones`, `DELETE /api/votantes/{id}`, `POST /api/votantes/importaciones/validar`, `POST /api/votantes/importaciones`, `POST /api/votantes/importaciones/reporte-errores` | Solo `ADMIN`; la reasignación identifica al líder por `liderDocumento` y el historial solo devuelve líder anterior, líder nuevo, motivo y fecha |
| Perfil | `PUT /api/perfil/me` | `ADMIN` y `LIDER`; edita sus propios datos, usuario y clave |
| Usuarios | `GET /api/usuarios`, `POST /api/usuarios/administradores`, `PATCH /api/usuarios/{id}/estado` | Solo `ADMIN`; las cuentas de líder se administran desde `/api/lideres` |
| Paneles | `GET /api/dashboard/admin`, `GET /api/dashboard/lider` | `ADMIN` o `LIDER`, respectivamente |
| Auditoría | `GET /api/auditoria` | Solo `ADMIN` |

Los listados aceptan paginación Spring (`page`, `size`, `sort`); el tamaño máximo es 100. `GET /api/votantes` también acepta filtros por documento de votante, nombres, apellidos, teléfono, estado, cabecera y documento del líder (`liderDocumento`); el documento del líder se busca dentro de la organización del administrador autenticado. Las consultas de `LIDER` se limitan en el servidor a sus propios votantes.

`PUT /api/perfil/me` recibe nombres, apellidos, username y opcionalmente email y teléfono (el teléfono solo se aplica a líderes). Si email o teléfono se omiten, se conserva el valor actual; enviarlos vacíos los borra. Para cambiar la clave, envíe `nuevaClave`; basta con tener una sesión autenticada. Cambiar usuario o clave requiere iniciar sesión nuevamente con las credenciales actualizadas. El rol y el documento del líder no se pueden modificar desde este endpoint.

La importación acepta archivos `.csv` y `.xlsx` de hasta 5 MB. Requiere los encabezados `tipoDocumento`, `numeroDocumento`, `nombres`, `apellidos` y `mesaVotacion`; también admite `telefono`, `departamentoVotacion` (o `departamento`), `municipioVotacion` (o `municipio`), `puestoVotacion` (o `puesto`), `direccion`, `barrio`, `estado` y `observaciones`. Departamento, municipio, puesto y dirección identifican la ubicación del puesto de votación; no son datos del domicilio del votante. No se recopila email del votante. Los documentos de líderes y votantes son únicos dentro de la organización de un administrador y pueden repetirse en organizaciones distintas. Las rutas de validar, importar y descargar el reporte requieren `liderId` además del archivo multipart `archivo`; el líder debe pertenecer al administrador autenticado. La importación devuelve errores identificados por fila; el reporte descarga las filas que no pasan la validación previa sin guardar votantes.

El alta y la actualización de votantes requieren `tipoDocumento`, `numeroDocumento`, `nombres`, `apellidos` y `mesaVotacion`; los demás datos de contacto y ubicación son opcionales.
