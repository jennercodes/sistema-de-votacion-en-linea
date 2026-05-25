# Resumen Técnico — Sistema de Votación en Línea

## ¿Qué es?

Aplicación web Jakarta EE que permite **crear y administrar encuestas**, **emitir votos** y **consultar resultados en tiempo real**. Desarrollada como proyecto del curso "Desarrollo Web Integrado".

El sistema cubre dos perfiles de uso:

- **Público:** lista de encuestas activas → seleccionar opción → emitir voto → resultados.
- **Administración:** CRUD completo de encuestas con opciones dinámicas (2 a 6 por encuesta) y activar/desactivar.

---

## Stack tecnológico

| Capa              | Tecnología                                                |
| :---------------- | :-------------------------------------------------------- |
| **Lenguaje**      | Java 17                                                   |
| **Plataforma**    | Jakarta EE 10 (Servlet 6.1, CDI 4.0, Faces 4.0)           |
| **Servidor**      | Apache Tomcat 10                                          |
| **Vistas**        | JSF/Facelets (Mojarra) + PrimeFaces 13 (tema saga)        |
| **CSS utilities** | PrimeFlex 3                                               |
| **Beans**         | CDI Managed Beans con Weld (`@Named` + `@ViewScoped`)     |
| **Persistencia**  | JDBC plano, sin ORM (`PreparedStatement`, transacciones)  |
| **Base de datos** | MySQL 8                                                   |
| **Build**         | Maven (empaquetado WAR)                                   |

---

## Arquitectura por capas

```text
            Browser (PrimeFaces JS + saga theme)
                │  HTTP / AJAX partial
                ▼
        ┌──────────────────────┐
        │   FacesServlet       │   ← jakarta.faces.webapp.FacesServlet
        └──────────────────────┘
                │ delega a managed beans (CDI)
                ▼
        ┌──────────────────────┐
        │   bean/  (@Named     │
        │          @ViewScoped)│   ← EncuestaBean, VotacionBean
        └──────────────────────┘
                │
                ▼
        ┌──────────────────────┐
        │   dao/  (JDBC)       │   ← EncuestaDAO, VotoDAO
        └──────────────────────┘
                │
                ▼
        ┌──────────────────────┐
        │   util/DBConnection  │   ← env vars con fallback
        └──────────────────────┘
                │
                ▼
        ┌──────────────────────┐
        │   MySQL 8            │
        └──────────────────────┘
```

No hay servlets propios — `FacesServlet` orquesta el ciclo JSF y los beans son los controladores efectivos. La capa DAO encapsula todo el acceso JDBC; los beans no conocen SQL.

---

## Estructura de paquetes

```text
src/main/java/com/votacion/
├── bean/
│   ├── EncuestaBean.java        # CRUD de encuestas, edición de opciones, navegación
│   └── VotacionBean.java        # Lista pública, registro de voto, resultados
├── dao/
│   ├── EncuestaDAO.java         # CRUD transaccional de encuestas + opciones
│   └── VotoDAO.java             # Registrar voto + agregación de resultados
├── model/
│   ├── Encuesta.java            # POJO + List<Opcion>
│   └── Opcion.java              # POJO (id, texto, orden)
└── util/
    └── DBConnection.java        # DriverManager + env vars

src/main/webapp/
├── index.xhtml                  # Dashboard público
├── votar.xhtml                  # Votación + resultados inline
├── resultados.xhtml             # Resultados independientes
├── admin/
│   ├── encuestas.xhtml          # Listado admin + acciones
│   └── formulario.xhtml         # Crear / editar encuesta con opciones AJAX
└── WEB-INF/
    ├── web.xml                  # FacesServlet + Weld listener
    ├── beans.xml                # CDI bean-discovery-mode="annotated"
    └── faces-config.xml         # placeholder (sin reglas custom)
```

---

## Base de datos

Esquema normalizado en tres tablas (`db/schema.sql`):

| Tabla       | Campos clave                                                                              |
| ----------- | ----------------------------------------------------------------------------------------- |
| `encuesta`  | `id` PK, `titulo`, `descripcion`, `activa`, `fecha_creacion`                              |
| `opciones`  | `id` PK, `encuesta_id` FK → `encuesta.id` (ON DELETE CASCADE), `texto`, `orden`           |
| `votos`     | `id` PK, `encuesta_id` FK CASCADE, `opcion_id` FK → `opciones.id` CASCADE, `nombre_votante` (nullable), `fecha` |

Borrar una encuesta elimina en cascada sus opciones y votos. El script incluye datos de ejemplo (encuesta de frameworks web con votos pre-cargados).

---

## Flujo de la aplicación

### Flujo público — Emitir un voto

1. Usuario abre `GET /votacion/` (`index.xhtml`).
2. `VotacionBean.init()` (`@PostConstruct`) carga las encuestas con `activa = true`.
3. `p:dataTable` lista las encuestas; cada fila tiene un `p:button outcome="votar"` con `f:param encuestaId`.
4. La vista `votar.xhtml` recibe el `encuestaId` vía `f:viewParam` y dispara `f:viewAction action="#{votacionBean.cargarEncuesta()}"`.
5. El usuario selecciona una opción (`p:selectOneRadio`) y opcionalmente su nombre (`p:inputText`).
6. Click en *Votar* → `p:commandButton action="#{votacionBean.votar()}"` con `update="@form"`.
7. `VotoDAO.registrar()` ejecuta `INSERT INTO votos (...)`.
8. `VotoDAO.obtenerResultados()` ejecuta el `SELECT` con `LEFT JOIN` y agregación.
9. La misma vista renderiza el `p:dataTable` de resultados con porcentaje, sin redirigir.

### Flujo admin — CRUD de encuestas

1. `GET /votacion/admin/encuestas.xhtml` → `EncuestaBean.init()` lista todas las encuestas.
2. *Nueva encuesta* / *Editar* → navegación GET a `admin/formulario.xhtml` con o sin `encuestaId`.
3. Edición de opciones (agregar / eliminar) usa `p:commandButton` con `update="@form"` (request AJAX, `execute="@form"` implícito): los textos ya tipeados se preservan porque el ciclo completo de JSF actualiza el modelo antes de invocar la acción.
4. *Guardar* → `EncuestaBean.guardar()` valida, llama a `EncuestaDAO.guardar()` (insert/update transaccional con `setAutoCommit(false)` + rollback en error), y redirige con `?faces-redirect=true` a `/admin/encuestas`.
5. *Eliminar* → `p:confirm` muestra un `p:confirmDialog` global; aceptado, `EncuestaDAO.eliminar()` borra la encuesta (CASCADE elimina opciones y votos).
6. *Activar / Desactivar* → toggle de `activa` con `UPDATE encuesta SET activa = ? WHERE id = ?`.

---

## Decisiones técnicas destacables

* **JSF + CDI sin Spring ni ORM:** todo se sostiene sobre la stack estándar de Jakarta EE 10. CDI/Weld está habilitado vía `beans.xml` y un listener en `web.xml`. Los beans se anotan con `@Named @ViewScoped` (estado por vista, sobrevive postbacks AJAX).
* **`<f:metadata>` como hijo directo del view:** evita la advertencia `JSF1103` y garantiza que `f:viewParam` + `f:viewAction` se procesen en la fase correcta.
* **Outcomes JSF absolutos:** todas las navegaciones admin usan `outcome="/admin/..."` o `outcome="/index"` para resolver correctamente desde cualquier subdirectorio (`/admin/`).
* **`<f:ajax execute="@form" render="@form">` en editor dinámico de opciones:** corrige un bug clásico del uso ingenuo de `immediate="true"`, donde los inputs no se escribían al bean antes de invocar la acción.
* **Transacciones explícitas en `EncuestaDAO.guardar()`:** `setAutoCommit(false)` + `commit()` / `rollback()` para mantener consistencia entre `encuesta` y `opciones`.
* **`PreparedStatement` en todas las consultas:** previene SQL Injection; los parámetros nunca se concatenan al SQL.
* **`try-with-resources` consistente:** asegura cierre automático de `Connection`, `PreparedStatement` y `ResultSet`.
* **Credenciales DB por variables de entorno:** `DBConnection` lee `DB_URL`, `DB_USER`, `DB_PASSWORD` desde `System.getenv()` con fallback a defaults locales (`localhost:3306`, `root`, vacío).
* **`Statement.RETURN_GENERATED_KEYS`** para recuperar el `id` autoincremental tras insertar una encuesta.
* **`LinkedHashMap` en resultados:** preserva el orden devuelto por la BD (`ORDER BY total DESC, orden ASC`).

---

## Frontend (PrimeFaces 13)

* **Tema saga** servido vía `<h:outputStylesheet library="primefaces-saga" name="theme.css"/>` (incluido en el JAR de PrimeFaces 13).
* **Layout en card** centrado: `<div class="card" style="margin: 2rem auto; max-width: 900px; padding: 1.5rem">` en cada vista, con `<style>` mínimo inline para tipografía heredada de la variable CSS `--font-family` de saga.
* **Componentes utilizados:** `p:dataTable` (con paginación en admin), `p:column`, `p:button`, `p:commandButton`, `p:inputText`, `p:inputTextarea`, `p:selectOneRadio`, `p:messages`, `p:confirm` + `p:confirmDialog`.
* **PrimeIcons** para acciones: `pi-pencil`, `pi-trash`, `pi-power-off`, `pi-plus`, `pi-save`, `pi-times`, `pi-check`, `pi-home`, `pi-cog`, `pi-arrow-right`.

---

## URLs de la aplicación

| URL                                      | Método   | Descripción                                |
| :--------------------------------------- | :------- | :----------------------------------------- |
| `/votacion/`                             | `GET`    | Dashboard público de encuestas activas     |
| `/votacion/votar.xhtml?encuestaId=<id>`  | `GET`    | Cargar votación + emitir voto (POST AJAX)  |
| `/votacion/resultados.xhtml`             | `GET`    | Vista independiente de resultados          |
| `/votacion/admin/encuestas.xhtml`        | `GET`    | Panel admin                                |
| `/votacion/admin/formulario.xhtml`       | `GET`    | Crear nueva encuesta                       |
| `/votacion/admin/formulario.xhtml?encuestaId=<id>` | `GET` | Editar encuesta existente              |

Las mutaciones (votar, guardar, eliminar, toggle) viajan como POST/AJAX al mismo path JSF — no hay endpoints REST separados.

---

## Lo que el proyecto demuestra

1. **Arquitectura por capas** con responsabilidades claras (model / dao / bean / vistas).
2. **Ciclo completo JSF**: viewParam → viewAction → bean → DAO → BD → render parcial AJAX.
3. **Migración real de Servlet/JSP a JSF/Facelets + PrimeFaces** manteniendo CRUD y persistencia funcional.
4. **JDBC con buenas prácticas:** PreparedStatement, try-with-resources, transacciones manuales, `RETURN_GENERATED_KEYS`.
5. **CDI en contenedor servlet** (Weld sobre Tomcat 10, no contenedor EE full) — minimal viable Jakarta EE.
6. **Componentes UI ricos sin escribir JavaScript propio:** PrimeFaces aporta paginación, modales de confirmación, AJAX parcial y mensajes.
7. **Configuración 12-factor parcial:** credenciales BD por variables de entorno.
