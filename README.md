# Backend de gestión de campaña electoral

API REST construida con Java 17, Spring Boot 3.x, Maven, Spring Security, JWT, Spring Data JPA y MySQL. Conserva las rutas y reglas de acceso existentes. La organización principal está bajo `com.campania` (`config`, `controller`, `dto`, `entity`, `exception`, `mapper`, `repository`, `security`, `service`, `specification` y `util`).

Hibernate mantiene el esquema con `spring.jpa.hibernate.ddl-auto=update`; Flyway, Liquibase y scripts SQL de migración no se usan. Para una base ya existente, respalda los datos antes de iniciar: Hibernate actualiza columnas y tablas de forma no destructiva, pero no sustituye cambios de datos de migraciones previas. El bootstrap de administrador opcional crea un usuario inicial mediante JPA, sin insertar credenciales por defecto.

## Requisitos

- JDK 17 o posterior.
- Maven (incluido mediante Maven Wrapper).
- MySQL 8, o Docker con Docker Compose.

## Ejecución con Docker Compose

1. Copia `.env.example` a `.env` y reemplaza `JWT_SECRET` y `BOOTSTRAP_ADMIN_PASSWORD` por valores propios seguros. El administrador inicial se crea solo si `BOOTSTRAP_ADMIN_ENABLED=true`.
2. Ejecuta `docker compose up --build`.
3. La API queda disponible en `http://localhost:8080`; MySQL usa la base `sigiv`, usuario `appuser`, contraseña `DB_PASSWORD` y volumen persistente. El estado de la base puede consultarse en `http://localhost:8080/actuator/health`.

## Ejecución local

Configura las variables requeridas antes de iniciar (PowerShell):

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/sigiv?serverTimezone=UTC"
$env:DB_USER = "appuser"
$env:DB_PASSWORD = "<contraseña-local>"
$env:JWT_SECRET = "<secreto-aleatorio-de-al-menos-32-bytes>"
$env:BOOTSTRAP_ADMIN_ENABLED = "true"
$env:BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BOOTSTRAP_ADMIN_PASSWORD = "<contraseña-segura>"
.\mvnw.cmd spring-boot:run
```

Hibernate crea y actualiza las tablas al iniciar. La cuenta inicial es opcional; si ya existe el nombre configurado, no se modifica. También se pueden configurar `PORT` (8080 por defecto), `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`, `OCR_TESSDATA_PATH` y `OCR_LANGUAGES`.

### Git Bash / MINGW64

Define las variables requeridas en la misma terminal antes de ejecutar Maven. Ajusta URL, usuario y contraseña para que coincidan con tu instancia local de MySQL; genera un secreto JWT aleatorio y no lo publiques:

```bash
export DB_URL='jdbc:mysql://localhost:3306/sigiv?serverTimezone=UTC'
export DB_USER='appuser'
export DB_PASSWORD='apppass'
export JWT_SECRET="$(openssl rand -base64 48)"
mvn clean spring-boot:run -U
```

Estas variables solo duran mientras siga abierta esa terminal. Si MySQL no está iniciado, levántalo primero o usa `docker compose up --build` después de configurar `.env`.

## Pruebas y empaquetado

Las pruebas usan H2 en memoria y no requieren MySQL:

```powershell
.\mvnw.cmd test
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
| Cabeceras | `GET/POST /api/cabeceras`, `GET/PUT /api/cabeceras/{id}`, `PATCH /api/cabeceras/{id}/estado` | `ADMIN`; el listado admite filtro parcial y sin distinción de mayúsculas con `nombre` |
| Líderes | `GET/POST /api/lideres`, `GET/PUT /api/lideres/{id}`, `PATCH /api/lideres/{id}/estado`, `GET /api/lideres/me` | Gestión `ADMIN`; perfil propio `LIDER`; el administrador puede restablecer la clave del líder al actualizarlo |
| Posibles votantes | `GET/POST /api/votantes`, `POST /api/votantes/administrador`, `GET /api/votantes/{id}`, `GET /api/votantes/documento/{documento}`, `PUT /api/votantes/{id}`, `PATCH /api/votantes/{id}/estado` | Consulta autenticada; registro `LIDER` o `ADMIN` (para `ADMIN`, asigna por documento del líder) |
| OCR de captura | `POST /api/votantes/ocr/censo` | `ADMIN` o `LIDER`; recibe imagen `PNG`/`JPEG` como multipart `imagen` y devuelve campos detectados para revisión |
| Asignaciones e importación | `PUT /api/votantes/{id}/lider`, `GET /api/votantes/{id}/historial-asignaciones`, `DELETE /api/votantes/{id}`, `POST /api/votantes/importaciones/validar`, `POST /api/votantes/importaciones`, `POST /api/votantes/importaciones/reporte-errores` | Solo `ADMIN`; la reasignación identifica al líder por `liderDocumento` y el historial solo devuelve líder anterior, líder nuevo, motivo y fecha |
| Perfil | `PUT /api/perfil/me` | `ADMIN` y `LIDER`; edita sus propios datos, usuario y clave |
| Usuarios | `GET /api/usuarios`, `POST /api/usuarios/administradores`, `PATCH /api/usuarios/{id}/estado` | Solo `ADMIN`; las cuentas de líder se administran desde `/api/lideres` |
| Paneles | `GET /api/dashboard/admin`, `GET /api/dashboard/lider` | `ADMIN` o `LIDER`, respectivamente |
| Auditoría | `GET /api/auditoria` | Solo `ADMIN` |

Los listados aceptan paginación Spring (`page`, `size`, `sort`); el tamaño máximo es 100. `GET /api/cabeceras` acepta el parámetro opcional `nombre` para buscar coincidencias parciales, sin distinguir mayúsculas y minúsculas (por ejemplo, `?nombre=centro` puede devolver “Centro Norte” y “Centro Sur”). `GET /api/votantes` también acepta filtros por documento de votante, nombres, apellidos, teléfono, estado, cabecera y documento del líder (`liderDocumento`); el documento del líder se busca dentro de la organización del administrador autenticado. Las consultas de `LIDER` se limitan en el servidor a sus propios votantes.

`PUT /api/perfil/me` recibe nombres, apellidos, username y opcionalmente email y teléfono (el teléfono solo se aplica a líderes). Si email o teléfono se omiten, se conserva el valor actual; enviarlos vacíos los borra. Para cambiar la clave, envíe `nuevaClave`; basta con tener una sesión autenticada. Cambiar usuario o clave requiere iniciar sesión nuevamente con las credenciales actualizadas. El rol y el documento del líder no se pueden modificar desde este endpoint.

Al actualizar un líder con `PUT /api/lideres/{id}`, el administrador puede incluir `nuevaClave` (de 12 a 72 caracteres) para restablecer su acceso. Si se omite, la clave del líder no cambia. El endpoint solo permite gestionar líderes de la organización del administrador autenticado.

`POST /api/votantes/ocr/censo` recibe `multipart/form-data` con el campo `imagen` y un token Bearer. Acepta capturas PNG/JPEG de hasta 5 MB y extrae NUIP, departamento, municipio, puesto, dirección y mesa; no registra ni almacena la imagen o los datos. La respuesta incluye `textoDetectado` y una confianza estimada; siempre revise y corrija los campos antes de enviarlos al endpoint de registro. Envíe la captura recortada alrededor de la tabla para reducir lecturas adicionales. Para habilitar el OCR, instale los modelos Tesseract `spa.traineddata` y `eng.traineddata` en la carpeta `tessdata` de la aplicación, o configure `OCR_TESSDATA_PATH` con la ruta de esa carpeta. `OCR_LANGUAGES` permite ajustar los idiomas (por defecto `spa+eng`). Ejemplo: `curl -X POST http://localhost:8080/api/votantes/ocr/censo -H "Authorization: Bearer <token>" -F "imagen=@captura.png"`.

La importación acepta archivos `.csv` y `.xlsx` de hasta 5 MB. Requiere los encabezados `tipoDocumento`, `numeroDocumento`, `nombres`, `apellidos` y `mesaVotacion`; también admite `telefono`, `departamentoVotacion` (o `departamento`), `municipioVotacion` (o `municipio`), `puestoVotacion` (o `puesto`), `direccion`, `barrio`, `estado` y `observaciones`. Departamento, municipio, puesto y dirección identifican la ubicación del puesto de votación; no son datos del domicilio del votante. No se recopila email del votante. Los documentos de líderes y votantes son únicos dentro de la organización de un administrador y pueden repetirse en organizaciones distintas. Las rutas de validar, importar y descargar el reporte requieren `liderId` además del archivo multipart `archivo`; el líder debe pertenecer al administrador autenticado. La importación devuelve errores identificados por fila; el reporte descarga las filas que no pasan la validación previa sin guardar votantes.

El alta y la actualización de votantes requieren `tipoDocumento`, `numeroDocumento`, `nombres`, `apellidos` y `mesaVotacion`; los demás datos de contacto y ubicación son opcionales.
