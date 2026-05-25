# Sistema de Votación en Línea

Aplicación web Jakarta EE que permite crear y administrar encuestas, emitir votos y consultar resultados en tiempo real. Construida con vistas JSF (Facelets) sobre Managed Beans CDI y persistencia JDBC directa contra MySQL.

## Tecnologías

- Java 17 + Jakarta EE 10
- Jakarta Faces 4.0 (Mojarra)
- CDI 4.0 (Weld) sobre Apache Tomcat 10
- JDBC plano (sin ORM)
- MySQL 8
- Maven (empaquetado WAR)

## Arquitectura por capas

```
src/main/java/com/votacion/
├── model/   POJOs de dominio (Encuesta, Opcion)
├── dao/     Acceso JDBC (EncuestaDAO, VotoDAO) + DBConnection
└── bean/    Managed Beans CDI (EncuestaBean, VotacionBean) — @Named @ViewScoped

src/main/webapp/
├── index.xhtml, votar.xhtml, resultados.xhtml
├── admin/encuestas.xhtml, admin/formulario.xhtml
└── WEB-INF/  web.xml, faces-config.xml, beans.xml
```

| Capa     | Responsabilidad                                                            |
| -------- | -------------------------------------------------------------------------- |
| `model`  | Entidades simples, sin lógica de negocio                                   |
| `dao`    | Consultas SQL, transacciones, mapeo `ResultSet` → POJO                     |
| `bean`   | Estado de vista, orquestación, validaciones, navegación JSF                |
| `vistas` | Facelets con `h:`, `f:`, `ui:`; `f:viewParam` + `f:viewAction` para estado |

## Base de datos

Tres tablas relacionadas en `votacion_db`:

- **`encuesta`** — `id`, `titulo`, `descripcion`, `activa`, `fecha_creacion`
- **`opciones`** — `id`, `encuesta_id` (FK → `encuesta.id`, `ON DELETE CASCADE`), `texto`, `orden`
- **`votos`** — `id`, `encuesta_id` (FK CASCADE), `opcion_id` (FK → `opciones.id` CASCADE), `nombre_votante`, `fecha`

Borrar una encuesta elimina en cascada sus opciones y votos. Script completo en `db/schema.sql` con datos de ejemplo.

## Ejecución

### Requisitos
- JDK 17
- Maven 3.9+
- MySQL 8 corriendo en `localhost:3306`
- Apache Tomcat 10
- IntelliJ IDEA (recomendado)

### Pasos
1. Clonar el repo y abrir como proyecto Maven en IntelliJ.
2. Ejecutar `db/schema.sql` en MySQL para crear `votacion_db` y datos iniciales.
3. Configurar las variables de entorno de conexión (ver sección siguiente) — opcional si los valores por defecto sirven.
4. Configurar un *Run Configuration* de Tomcat 10 en IntelliJ apuntando al artefacto `votacion:war exploded` (context path `/votacion`).
5. Iniciar Tomcat y abrir [http://localhost:8080/votacion/](http://localhost:8080/votacion/).

### Configuración de la base de datos

`DBConnection` lee tres variables de entorno con fallback a valores de desarrollo local:

| Variable      | Default                                       | Descripción                          |
| ------------- | --------------------------------------------- | ------------------------------------ |
| `DB_URL`      | `jdbc:mysql://localhost:3306/votacion_db`     | URL JDBC de la base                  |
| `DB_USER`     | `root`                                        | Usuario MySQL                        |
| `DB_PASSWORD` | *(vacío)*                                     | Contraseña MySQL                     |

**Cómo configurarlas en IntelliJ antes de desplegar:**

1. Abrir *Run / Edit Configurations…* y seleccionar la configuración de Tomcat.
2. En la pestaña *Startup/Connection* o en el bloque *Environment variables* del *Run Configuration*, definir cada variable (`DB_URL=…`, `DB_USER=…`, `DB_PASSWORD=…`).
3. Aplicar y reiniciar Tomcat para que las variables se inyecten al proceso.

Alternativamente, exportarlas en la shell antes de lanzar el servidor:

```bash
export DB_URL='jdbc:mysql://localhost:3306/votacion_db'
export DB_USER='votacion_user'
export DB_PASSWORD='secret'
```

## Funcionalidades del avance 2

- **CRUD de encuestas** con opciones dinámicas (2–6 por encuesta), título y descripción.
- **Dashboard público** que lista únicamente encuestas con `activa = true`.
- **Flujo de votación** con resultados inline (porcentaje y conteo) tras emitir el voto.
- **Panel de administración** con crear, editar, eliminar y activar/desactivar.
- **Migración completa a JSF** sobre `@Named @ViewScoped`, navegación entre vistas vía `f:viewParam` + `f:viewAction`.

## Navegación

| Vista                     | Propósito                                                            |
| ------------------------- | -------------------------------------------------------------------- |
| `index.xhtml`             | Dashboard público de encuestas activas                               |
| `votar.xhtml`             | Formulario de votación + resultados inline tras emitir el voto       |
| `resultados.xhtml`        | Vista independiente de resultados de la encuesta actual              |
| `admin/encuestas.xhtml`   | Listado administrativo con acciones (editar, eliminar, toggle)       |
| `admin/formulario.xhtml`  | Crear o editar encuesta con opciones dinámicas                       |

Navegación entre vistas: `h:button outcome="..."` con `f:param` para pasar `encuestaId`; la vista destino lo recibe con `f:viewParam` y dispara `f:viewAction` para cargar estado desde el DAO.

## Checklist de avances

- [x] **Avance 1** — Estructura base del proyecto, servlets de ejemplo, JSPs iniciales y conexión JDBC.
- [x] **Avance 2** — Arquitectura por capas, esquema normalizado, CRUD de encuestas, migración a JSF + CDI con Managed Beans `@ViewScoped`.
- [ ] **Avance 3** — Pendiente.
- [ ] **Avance 4** — Pendiente.

## Evidencias

Agregar capturas en `doc/` y referenciarlas aquí:

- Dashboard público (`index.xhtml`)
- Formulario de creación de encuesta (`admin/formulario.xhtml`)
- Flujo de votación: selección de opción → resultados inline (`votar.xhtml`)
