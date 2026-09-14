# Colección de Bruno — Tucán API

Las peticiones que se usan a diario mientras se desarrolla, listas para ejecutar.
Bruno guarda todo en texto plano, así que esta colección viaja versionada junto al
código: quien clone el repo obtiene las peticiones.

## Cómo usarla

1. Abrir Bruno → **Open Collection** → elegir esta carpeta (`bruno/`).
2. Arriba a la derecha, seleccionar el entorno **Local**.
3. Pegar la API key en la variable `apiKey` del entorno (lápiz junto al selector).
4. Levantar la API (`./mvnw spring-boot:run`) y ejecutar las peticiones en orden.

## La API key nunca se versiona

`apiKey` está declarada como **variable secreta**. Bruno guarda su valor cifrado
fuera de la colección y no lo escribe en los archivos `.bru`, así que esta carpeta
se puede subir a GitHub sin filtrar nada.

Por eso hay que pegar la key a mano en cada máquina: no viene en el repo, y no debe
venir.

## Las peticiones

| Petición | Método | Qué comprueba |
|---|---|---|
| `01 - Ping` | GET | 200 y el cuerpo `pong`. Es la única ruta sin API key |
| `02 - Categorias` | GET | 200, y que vengan las ramas `INGRESO` y `GASTO` |
| `03 - Crear gasto` | POST | 201 y el mensaje de confirmación |
| `04 - Crear ingreso` | POST | 201, sin descripción (columna E vacía) |

Cada una lleva sus `assert`, así que **Run** las verifica solas.

## Lo que ningún assert puede verificar

El criterio de aceptación de verdad de FIN-21 está en la hoja, no en la respuesta:

- Abrir Google Sheets y confirmar que apareció la fila nueva.
- La fecha (columna A) y el monto (columna C) tienen que quedar alineados a la
  **derecha**. Si se ven a la izquierda, entraron como texto y hay que revisar la
  opción de entrada de valores de FIN-17.
- En el ingreso, la columna E (descripción) tiene que quedar vacía.

## Nota sobre el cuerpo del ingreso

El "Contrato de la API" del backlog muestra el ingreso sin categoría. Ese ejemplo
quedó viejo: desde FIN-13 la categoría es obligatoria en ingresos y gastos, y desde
FIN-14 tiene que corresponder al tipo. Un ingreso sin categoría hoy responde 400.
Por eso `04 - Crear ingreso` manda `"categoria": "Salario"`, igual que la fila de
ingreso de la tabla de estructura de la hoja.
