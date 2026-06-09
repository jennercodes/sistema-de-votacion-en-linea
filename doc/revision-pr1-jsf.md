# Revisión del PR #1 y validación de conocimiento JSF/PrimeFaces

> **PR revisado:** [#1 — "Mejoras en formularios y análisis JSF"](https://github.com/jennercodes/sistema-de-votacion-en-linea/pull/1)
> (autor `joseph17-max`, base `main`, head `feature/mejoras-formularios`, **mergeado** el 2026-06-08).
> **Rama de corrección:** `fix/jsf-message-target-null` (este trabajo).
> **Alcance del PR:** 7 archivos, +381 / −88. Dos beans CDI y las cinco vistas Facelets.

Este documento hace dos cosas: (1) **revisa** lo que el PR #1 aportó, y (2) **valida con el código real** el conocimiento de JSF/PrimeFaces que explica tanto las mejoras como los tres bugs de regresión que el PR introdujo y que esta rama corrige.

---

## 1. Qué aportó el PR #1 (lo bueno)

| Área | Cambio | Valor |
| :--- | :--- | :--- |
| `VotacionBean` | Métodos `cargarResultados()`, `tieneResultados()`, `obtenerOpcionGanadora()`, `obtenerVotoMaximo()` | Habilita la vista `resultados.xhtml` independiente y los resúmenes (ganador, máximo). |
| `EncuestaBean` | `getTotalEncuestas()`, `getEncuestasActivas()`, `getEncuestasInactivas()`, `puedeAgregarOpcion()`, `puedeEliminarOpcion()` | Tarjetas de estadísticas en el panel admin y `disabled` declarativo en los botones de opción. |
| `resultados.xhtml` | `<f:metadata>` con `f:viewParam` + `f:viewAction="#{votacionBean.cargarResultados()}"` | La vista ahora se puede abrir directo por URL con `?encuestaId=`. |
| `index.xhtml` / `admin/encuestas.xhtml` | `p:dataTable` con paginación, filtro, orden, badges, columna Estado con color | UX notablemente mejor sobre las tablas planas anteriores. |
| Vistas en general | Barras de progreso (`progress-fill`), badge de ganador, layout en grid | Resultados mucho más legibles. |

El PR es una mejora real de UX. El problema está en **cómo** introdujo los mensajes y algunos botones.

---

## 2. Regresiones introducidas por el PR (los tres bugs)

### Bug A — `<p:message>` sin `for` → la página entera revienta

**Síntoma observado** (página de error de Mojarra/Facelets):

```
Cannot invoke "jakarta.faces.component.UIComponent.getClientId(jakarta.faces.context.FacesContext)"
because "target" is null
```

El PR reemplazó los avisos de texto plano por `p:message` en cinco lugares. Diff representativo:

```diff
- <p style="color: #1b5e20;">#{encuestaBean.mensajeExito}</p>
+ <p:message severity="info" text="#{encuestaBean.mensajeExito}" closable="true"/>
```

**Causa raíz (conocimiento validado):** `<p:message>` (igual que `<h:message>`) es un componente **por-componente**: muestra los `FacesMessage` asociados a *un* control concreto identificado por su atributo obligatorio `for`. Internamente el renderer resuelve el target con el `for` y llama `target.getClientId(context)`. Si no hay `for`, el target se resuelve a `null` y `null.getClientId(...)` lanza el NPE exacto del síntoma. **No existe un modo "global" de `p:message`** — para mensajes globales se usa `<p:messages>` (plural), que sí puede ir sin `for`.

Estos mensajes nunca fueron de validación de campo: eran cajas informativas/estado. El componente correcto no era `p:message`.

**Por qué rompía dos flujos distintos:**

- `resultados.xhtml`: el `p:message severity="info"` se renderiza siempre que no hay resultados → NPE en el render inicial → **página de error completa**.
- `votar.xhtml`: el `p:message severity="success"` vive en la rama `rendered="#{votacionBean.votoRegistrado}"`. Al emitir el voto con `update="@form"`, esa rama se vuelve visible y el render parcial AJAX lanza el NPE → la respuesta parcial falla y **el `<form>` queda en blanco** (solo sobrevive el `<h1>`, que está fuera del form). Esto es exactamente el "al emitir un voto no sale nada más".

### Bug B — flechas literales en el texto de los botones

```diff
- <p:button value="Volver al inicio" icon="pi pi-home" .../>
+ <p:button value="← Volver al inicio" icon="pi pi-home" .../>
+ <p:button value="Ver encuestas →"  icon="pi pi-list" .../>
```

El resultado renderizado era `🏠 ← Volver al inicio`: el botón ya lleva un **icono PrimeIcon** (`pi-home`, `pi-list`) y, además, una flecha Unicode incrustada en el `value`. Doble indicador, visualmente sucio y redundante.

### Bug C — columna "Acciones" demasiado angosta → botones recortados

```diff
- <p:column headerText="Acciones" style="width: 10rem">    <!-- 1 botón -->
+ <p:column headerText="Acciones" style="width: 12rem">    <!-- 2 botones -->
+   <p:button value="Participar" .../>
+   <p:button value="Ver resultados" .../>
```

El PR metió un segundo botón ("Ver resultados") en la misma columna pero solo subió el ancho de 10rem a 12rem. Dos botones en línea no caben en 12rem → "Ver resultados" se desborda y queda cortado en el borde derecho de la tabla.

---

## 3. Conocimiento JSF/PrimeFaces validado contra esta feature

Cada punto está confirmado contra el código real del repo y/o el comportamiento observado en runtime.

### 3.1 `p:message` vs `p:messages`

| Componente | `for` | Uso correcto |
| :--- | :--- | :--- |
| `<p:message for="campo">` | **obligatorio** | mensajes de validación de **un** input |
| `<p:messages>` | no | todos los `FacesMessage` globales de la vista |
| Aviso estático/decorativo | n/a | HTML/CSS propio (`<div class="alert ...">`), no es un `FacesMessage` |

Regla práctica: si el texto **no** proviene de `FacesContext.addMessage(...)` y **no** está atado a un campo, no uses `p:message`. La corrección de esta rama reemplaza los cinco `p:message` decorativos por `<div class="alert alert-*">`.

### 3.2 Ciclo `@ViewScoped` + `f:viewParam` + `f:viewAction`

Ambos beans son `@Named @ViewScoped` (`jakarta.faces.view.ViewScoped`, scope de CDI). El estado vive mientras se permanezca en la misma vista, sobreviviendo postbacks AJAX.

- `<f:viewParam name="encuestaId" value="#{votacionBean.encuestaId}"/>` mapea el query param a la propiedad del bean.
- `<f:viewAction action="#{votacionBean.cargarResultados()}"/>` se ejecuta tras aplicar el viewParam, en `INVOKE_APPLICATION`.
- **Matiz clave:** `f:viewAction` corre por defecto **solo en GET inicial** (`onPostback="false"`). En los postbacks AJAX **no** se reejecuta; es el scope de vista quien preserva `encuestaActual`/`resultados`. Por eso es indispensable que el bean sea `@ViewScoped` y no `@RequestScoped`: con request scope, tras el AJAX de votar el estado se perdería y el `update="@form"` no encontraría qué renderizar.

`<f:metadata>` debe ser hijo directo del elemento raíz de la vista (antes de `<h:head>`), si no, los viewParam/viewAction no se procesan en la fase correcta.

### 3.3 `update="@form"` y por qué una excepción de render deja el form vacío

`p:commandButton ... action update="@form"` dispara un POST AJAX que **procesa** (`process="@form"` implícito) y **renderiza** (`update="@form"`) el formulario. La respuesta es un XML parcial con el HTML del form. Si **durante el render** del form salta una excepción (el `p:message` sin `for`), la respuesta parcial se corta y el navegador reemplaza el contenido del form por algo vacío/incompleto. De ahí el form en blanco tras votar. La lección: un error de render en cualquier subárbol incluido en `update` afecta a todo el bloque actualizado.

### 3.4 `PROJECT_STAGE=Development` explica la página de error

`WEB-INF/web.xml` declara:

```xml
<context-param>
    <param-name>jakarta.faces.PROJECT_STAGE</param-name>
    <param-value>Development</param-value>
</context-param>
```

En **Development**, Mojarra muestra la página "An Error Occurred" con *Stack Trace / Component Tree / Scoped Variables* y el pie "Generated by Mojarra/Facelets" — justo la captura del bug A. En **Production** el usuario vería solo un error genérico (o la `<error-page>` configurada). Útil para depurar en local; recordar cambiarlo antes de cualquier despliegue real.

### 3.5 `p:dataTable`: anchos de columna y desbordamiento

La tabla vive dentro de `.card { max-width: 900px }`. Con columnas de ancho fijo, la suma de anchos + el contenido no envuelto puede exceder el contenedor y recortarse. Dos opciones para acciones múltiples:
- apilar los botones en un contenedor flex en columna (lo que hace esta rama: `display:flex; flex-direction:column` en 13rem), o
- permitir `flex-wrap` con ancho suficiente.

`reflow="true"` ayuda en pantallas chicas pero no resuelve el desbordamiento horizontal de una columna fija con dos botones en línea.

### 3.6 Botones: iconos PrimeIcon, no flechas literales

Patrón correcto en PrimeFaces: la dirección/acción se comunica con `icon="pi pi-*"`, no con glifos Unicode en el `value`. Tras esta rama no queda ningún `←`/`→` en `value` ni ningún `pi-arrow*` (se mapearon y eliminaron todos).

### 3.7 Editor dinámico de opciones (`ui:repeat` + `commandButton update="@form"`)

`admin/formulario.xhtml` itera `opcionesTexto` con `<ui:repeat>` y cada fila tiene un `p:commandButton ... action="#{encuestaBean.eliminarOpcion(status.index)}" update="@form"`. Como `update="@form"` lleva `process="@form"` implícito, **los textos ya tipeados se escriben al modelo antes** de invocar la acción de agregar/eliminar, así que no se pierden. Los botones usan `disabled="#{!encuestaBean.puedeAgregarOpcion()}"` / `puedeEliminarOpcion()` para respetar el rango 2–6 opciones (`MIN_OPCIONES`/`MAX_OPCIONES` en `EncuestaBean`).

> Nota de precisión: `doc/resumen-tecnico.md` describe este mecanismo como `<f:ajax execute="@form" render="@form">`. El código real usa `p:commandButton update="@form"`, que es equivalente en efecto (process+render del form) pero no es un `<f:ajax>` explícito.

---

## 4. Correcciones aplicadas en `fix/jsf-message-target-null`

| Archivo | Problema (del PR #1) | Corrección |
| :--- | :--- | :--- |
| `votar.xhtml` | 3× `p:message` sin `for` (warn/error/success) | `<div class="alert alert-*">` |
| `resultados.xhtml` | 2× `p:message` sin `for` (info/success) | `<div class="alert alert-*">` |
| `admin/encuestas.xhtml` | 1× `p:message` sin `for` (info) | `<div class="alert alert-info">` |
| `admin/formulario.xhtml` | 2× `p:message` sin `for` (error/success) | `<div class="alert alert-*">` |
| `resultados.xhtml` | `value="← Volver al inicio"` / `"Ver encuestas →"` | sin flechas; icono `pi-list` unificado |
| `index.xhtml` | columna Acciones 12rem recorta "Ver resultados" | botones apilados en flex (13rem); `Participar` con `pi-check` en vez de `pi-arrow-right` |

Los `<p:messages>` globales (plural) se conservan: son válidos sin `for` y muestran los `FacesMessage` reales de cada vista. Se añadieron clases `.alert / .alert-info / .alert-success / .alert-warn / .alert-error` al `<style>` de cada vista afectada.

**Commits de la rama:**
- `fix(jsf): elimina p:message sin "for" que rompía votar y resultados`
- `fix(jsf): arregla botones recortados y quita flechas sobrantes`

---

## 5. Discrepancias detectadas en la documentación previa

Al validar contra el código actual (ya corregido), `doc/resumen-tecnico.md` quedó con dos imprecisiones menores:

1. Lista `pi-arrow-right` entre los PrimeIcons usados; tras esta rama **ya no se usa** (`Participar` pasó a `pi-check`, `resultados` a `pi-list`/`pi-home`).
2. Describe el editor de opciones como `<f:ajax execute="@form" render="@form">`; el código real usa `p:commandButton update="@form"` (ver §3.7).

No se modifica ese archivo aquí; quedan registradas para una futura pasada de sincronización.

---

## 6. Checklist de verificación manual

Tras desplegar el WAR (`target/votacion.war`) en Tomcat 10:

- [ ] `GET /votacion/resultados.xhtml?encuestaId=<id con votos>` → tabla con badges, ★ en el máximo y barras de %; **sin** página de error.
- [ ] `GET /votacion/resultados.xhtml?encuestaId=<id sin votos>` → caja `.alert-info` "Aún no hay resultados…" + botón "Ver encuestas"; **sin** NPE.
- [ ] `votar.xhtml?encuestaId=<id>` → seleccionar opción → "Emitir voto" → aparece `.alert-success` + tabla de resultados (el form **no** queda en blanco).
- [ ] `index.xhtml` → columna Acciones muestra "Participar" y "Ver resultados" completos, sin recorte.
- [ ] Ningún botón muestra flechas Unicode (`←`/`→`) en su etiqueta.
- [ ] `admin/formulario.xhtml`: guardar con error/éxito muestra `.alert-error`/`.alert-success`, no la página de error.
