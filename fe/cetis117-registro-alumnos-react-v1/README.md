# CETIS 117 - Portal React de asistencia

Aplicación web responsive para consulta de asistencia y administración de autorizaciones.

## Tecnologías

- React
- Vite
- Tailwind CSS
- Fetch API
- Backend Spring Boot desplegado en Cloud Run

## Flujo de acceso

Al cargar la aplicación se solicita el código de seguridad.

### Código de padre/tutor

Si el código pertenece a un tutor:
- solo se muestra el alumno relacionado;
- se muestran datos del alumno;
- datos del tutor;
- entradas;
- salidas;
- estado;
- autorización, nombre de quien autorizó y motivo cuando aplique.

### Código de usuario del sistema

Si pertenece a `usuariosSistema`:
- se muestra el módulo de consulta de alumnos;
- filtros por grupo, nombre e iniciales;
- módulo para autorizar salida;
- autorización por ALUMNO o GRUPO.

## Servicios consumidos

```text
GET  /api/acceso/validar
GET  /api/alumnos/consulta
GET  /api/grupos
POST /api/autorizaciones
```

## Backend predeterminado

```text
https://zdregistroalumnos-482614074090.us-central1.run.app
```

Para cambiarlo crea `.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

o usa otra URL de Cloud Run.

## Ejecutar localmente

### 1. Instalar Node.js

Recomendado Node 18 o superior.

Verifica:

```powershell
node -v
npm -v
```

### 2. Entrar al proyecto

```powershell
cd cetis117-registro-alumnos-react-v1
```

### 3. Instalar dependencias

```powershell
npm install
```

### 4. Crear `.env` opcional

Si usarás los servicios de Cloud Run, no es necesario.

Para backend local:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 5. Iniciar

```powershell
npm run dev
```

Vite mostrará una dirección similar a:

```text
http://localhost:5173
```

## Compilar producción

```powershell
npm run build
```

El resultado queda en:

```text
dist/
```

## Diseño

La interfaz usa la paleta del diseño de CETIS 117:
- guinda;
- dorado;
- crema;
- blanco;
- acentos verdes para estados exitosos.

El diseño cambia automáticamente para celular, tablet y laptop.


## Cambios v2

- En la lista administrativa se agregó el botón `Consultar registro`.
- Al seleccionar un alumno se abre la misma vista detallada que ve el tutor.
- Se agregó el módulo `Estado de salida por grupo`.
- El módulo muestra cuántos alumnos del grupo ya registraron salida durante el día.
- El selector de grupos ahora muestra únicamente el nombre del grupo, sin la hora.


## Cambios v3

- Se eliminó la columna `Último registro` de la tabla administrativa para mejorar el espacio disponible.
- En la vista móvil también se eliminó el dato de último registro de cada tarjeta.
- En el módulo `Autorizar salida` por grupo, el selector ahora muestra únicamente el nombre del grupo, sin la hora de salida.


## Cambios v4

- En la tabla administrativa se eliminó la columna `Tutor` para aprovechar mejor el espacio.
- En móvil también se retiró el nombre del tutor de la tarjeta resumida.
- En `Autorizar salida` por alumno, después de localizar al alumno se muestra el nombre del grupo en lugar del `idGrupo`.


## Cambios v5 - Estado de salida por grupo

La sección `Estado de salida por grupo` ahora usa el estado calculado por el backend.

Posibles estados:

- `Pendiente`
- `Salida por autorización`
- `Salida por horario`

La tarjeta ya no intenta deducir el estado contando registros de alumnos.


## Cambios v6 - Pantalla principal

- Se rediseñó la pantalla inicial donde se solicita el código.
- El nuevo diseño usa una composición tipo portada:
  - panel izquierdo visual con el logo grande de CETIS 117;
  - texto de bienvenida;
  - tarjetas informativas;
  - panel derecho con el formulario de acceso.
- Se eliminó cualquier referencia visual al logo amarillo de Zona D en la vista principal.
- El diseño sigue siendo responsive para laptop y celular.


## Cambios v7 - Logo y altura de inicio

- Se sustituyó el logo recortado anterior por el archivo original proporcionado.
- El logo usa `object-contain` para conservar sus proporciones sin deformarse.
- Se redujo el tamaño visual del logo.
- Se compactó la pantalla de acceso para reducir o evitar scroll vertical en laptops.
- Se redujeron paddings, textos y espacios sin afectar la versión responsive.


## Cambios v8 - Nuevo estilo visual

- Se adoptó un guinda más vivo y contrastante.
- La pantalla principal fue rediseñada con un estilo más cercano a la referencia visual compartida.
- Se usa el logo original del CETIS 117 proporcionado por el usuario.
- Las horas de entrada y salida ahora resaltan visualmente en la bitácora.
- Entrada usa una tarjeta clara con acento dorado.
- Salida usa una tarjeta clara con acento guinda.
- En móvil, entrada y salida también se muestran en bloques separados y fáciles de localizar.


## Cambios v9 - Ajustes de portada

- El bloque superior del header ahora queda cargado a la izquierda.
- El logo principal de la portada fue centrado dentro del panel izquierdo.
- La etiqueta `Portal escolar` se reposicionó para evitar que se corte.
- Se mantuvo el estilo visual de la versión anterior.
