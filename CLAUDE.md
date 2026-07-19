# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Online voting system (Sistema de Votación en Línea) — a Jakarta EE web app to create and manage polls, cast votes, and view results in real time. Course project for "Desarrollo Web Integrado." The app is functional: JSF/Facelets views (PrimeFaces), CDI managed beans, a plain-JDBC DAO layer, user authentication with roles, and a normalized MySQL schema are all in place.

## Tech Stack

- Java 17 (compiler target) on Jakarta EE 10
- **Jakarta Faces 4.0 (Mojarra)** + **PrimeFaces 13** (`jakarta` classifier, saga theme) + **PrimeFlex 3** — views are Facelets (`.xhtml`), not JSP
- **CDI 4.0 (Weld)** for managed beans (`weld-servlet-shaded`, since Tomcat is a plain servlet container without built-in CDI)
- Plain JDBC for persistence (no ORM)
- **MySQL 8** primary database; **H2 in-memory** as an opt-in fallback (see Database)
- **BCrypt** (`jbcrypt`) for password hashing
- Apache Tomcat 10 as the application server (cargo plugin can provision one)
- Maven, packaged as a WAR (`finalName` = `votacion`)
- JUnit 5 is on the classpath but **no tests exist yet**

## Build & Run Commands

> The Maven wrapper (`mvnw`) currently has **CRLF line endings**, so `./mvnw` fails on macOS/Linux with `bad interpreter: /bin/sh^M`. Use a globally-installed `mvn`, or convert the wrapper's line endings (`sed -i '' 's/\r$//' mvnw`) before using it.

```bash
mvn clean package                 # Build WAR -> target/votacion.war
mvn test                          # Run all tests (none yet)
mvn test -Dtest=ClassName         # Run a single test class
mvn package cargo:run             # Provision Tomcat 10.1.20 and run the app on :8080
```

The `cargo-maven3-plugin` (tomcat10x, port 8080) can download and run Tomcat automatically, so `mvn package cargo:run` is the fastest way to launch locally. App URL: `http://localhost:8080/votacion/`.

For IDE deployment, use an IntelliJ Tomcat 10 run configuration pointing at the `votacion:war exploded` artifact (context path `/votacion`).

## Architecture

Layered, under `com.votacion`:

- **model/** — domain POJOs (`Encuesta`, `Opcion`, `Usuario`, `Categoria`), no business logic
- **dao/** — plain-JDBC data access (`EncuestaDAO`, `VotoDAO`, `UsuarioDAO`, `CategoriaDAO`): SQL, transactions, `ResultSet` → POJO mapping
- **filter/** — `SecurityFilter`, an HTTP filter that protects `/admin/*` (authentication + role authorization)
- **bean/** — CDI managed beans (`EncuestaBean`, `VotacionBean`, `LoginBean`, `RegistroBean`): view/session state, orchestration, validation, JSF navigation
- **util/** — `DBConnection` (env-var-driven connection factory)

Views live in `src/main/webapp` (`index`, `login`, `registro`, `votar`, `resultados`, `admin/encuestas`, `admin/formulario`) with config under `WEB-INF/` (`web.xml`, `faces-config.xml`, `beans.xml`). Request flow: `FacesServlet` → managed bean (`@Named` + `@ViewScoped`) → DAO → `DBConnection` → MySQL/H2.

## Database

- **MySQL 8**, schema `votacion_db`, normalized to 3NF. Full script with seed data: `db/schema.sql`. Incremental migrations under `db/migrations/` (`V2__normalizacion.sql`, `V3__add_fecha_fin.sql`). H2 variant: `db/schema_h2.sql` / `src/main/resources/schema_h2.sql`.
- Tables: `usuario`, `categoria`, `encuesta`, `opciones`, `registro_participacion` (enforces single-vote), `votos`, `auditoria_admin`. Deleting a poll cascades to its options and votes.
- Seed users: `admin`/`admin123` (ADMIN), `juan`/`juan123`, `maria`/`maria123` (VOTANTE).

### Connection configuration (`util/DBConnection.java`)

Credentials come **only** from environment variables — there are no insecure hardcoded defaults (it no longer assumes `root` with a blank password). If a required variable is missing, class initialization fails with an explicit message.

| Variable         | Required                        | Description                                                        |
| ---------------- | ------------------------------- | ------------------------------------------------------------------ |
| `DB_URL`         | yes (non-empty)                 | JDBC URL, e.g. `jdbc:mysql://localhost:3306/votacion_db`           |
| `DB_USER`        | yes (non-empty)                 | MySQL user                                                         |
| `DB_PASSWORD`    | yes (may be empty, must be set) | MySQL password — empty allowed, but must be defined explicitly     |
| `DB_FALLBACK_H2` | no (default off)                | `true` to fall back to in-memory H2 if MySQL is unreachable        |

The H2 fallback is **opt-in**: without `DB_FALLBACK_H2=true`, a MySQL connection failure propagates instead of being silently masked. Set these in the Tomcat run configuration's environment, or export them in the shell before launching:

```bash
export DB_URL='jdbc:mysql://localhost:3306/votacion_db'
export DB_USER='votacion_user'
export DB_PASSWORD='secret'
# export DB_FALLBACK_H2=true   # optional: allow H2 fallback for local dev without MySQL
```

## Key Notes

- WAR packaging — no embedded server; requires external Tomcat 10 (or the cargo plugin).
- No Spring, no ORM — raw JSF + CDI + JDBC by design (course requirement).
- Views are JSF/Facelets + PrimeFaces, **not** JSP. CDI runs via Weld because Tomcat has no built-in CDI.
- The `mvnw` wrapper has CRLF line endings (broken on Unix) — prefer global `mvn` until it's converted.
- `README.md` still documents the old env-var-with-dev-defaults behavior; the hardened `DBConnection` above supersedes it.
