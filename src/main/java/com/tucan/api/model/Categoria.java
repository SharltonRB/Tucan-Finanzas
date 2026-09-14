package com.tucan.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Categorias permitidas para un movimiento: dos de ingreso y once de gasto.
 *
 * <p>La etiqueta es lo que se escribe en la columna D de la hoja, sin tildes y
 * con la capitalizacion exacta de su lista desplegable.
 *
 * <p>Cada categoria guarda el {@link TipoMovimiento} al que pertenece. Sin eso
 * nada impediria registrar un GASTO con categoria Salario, y el dashboard
 * quedaria sucio en silencio; con eso, la regla cruzada de FIN-14 es una simple
 * comparacion y el endpoint de categorias puede devolver la lista ya filtrada.
 *
 * <p>Este enum es el unico lugar donde se editan las categorias: agregar una es
 * agregar una linea aca y otra en el menu del atajo.
 */
public enum Categoria {

   SALARIO("Salario", TipoMovimiento.INGRESO),
   OTROS_INGRESOS("Otros ingresos", TipoMovimiento.INGRESO),

   ALIMENTACION("Alimentacion", TipoMovimiento.GASTO),
   COMIDA_CHATARRA("Comida Chatarra", TipoMovimiento.GASTO),
   TRANSPORTE("Transporte", TipoMovimiento.GASTO),
   ALQUILER("Alquiler", TipoMovimiento.GASTO),
   SERVICIOS("Servicios", TipoMovimiento.GASTO),
   SALUD("Salud", TipoMovimiento.GASTO),
   EDUCACION("Educacion", TipoMovimiento.GASTO),
   ENTRETENIMIENTO("Entretenimiento", TipoMovimiento.GASTO),
   ELECTRONICO("Electronico", TipoMovimiento.GASTO),

   /**
    * Va como GASTO, no como ingreso. Tratarla como ingreso duplicaria dinero: el
    * salario ya se anoto una vez y el traslado al ahorro lo contaria de nuevo.
    * Como gasto, el balance refleja la plata que queda disponible de verdad; el
    * costo es que la tasa de ahorro del resumen sale mas baja que la real.
    */
   AHORRO_INVERSION("Ahorro/Inversion", TipoMovimiento.GASTO),
   OTROS_GASTOS("Otros gastos", TipoMovimiento.GASTO);

   private final String etiqueta;
   private final TipoMovimiento tipo;

   Categoria(String etiqueta, TipoMovimiento tipo) {
      this.etiqueta = etiqueta;
      this.tipo = tipo;
   }

   public String getEtiqueta() {
      return etiqueta;
   }

   public TipoMovimiento getTipo() {
      return tipo;
   }

   /**
    * Resuelve el texto que llega en el JSON: acepta la etiqueta de la hoja, el
    * nombre de la constante, tildes, cualquier capitalizacion y espacios de sobra.
    *
    * <p>Un texto vacio devuelve {@code null} y lo rechaza despues la validacion,
    * con un mensaje mas util que el de una excepcion de deserializacion.
    */
   @JsonCreator
   public static Categoria desde(String valor) {
      return Etiquetas.resolver(values(), valor, "Categoria no reconocida");
   }

   /**
    * Las etiquetas agrupadas por tipo, que es lo que el atajo necesita para armar
    * sus dos menus. Con {@code filtro} nulo devuelve las dos ramas de una sola vez
    * y se ahorra una peticion; con un tipo, solo esa.
    *
    * <p>Vive aca y no en el controlador porque es una pregunta sobre el dominio, y
    * porque asi agregar una categoria sigue siendo una sola linea en este enum.
    *
    * <p>El {@link EnumMap} y el orden de encuentro del {@code groupingBy} hacen que
    * las etiquetas salgan en el orden de declaracion, que es el orden en que se ven
    * en la pantalla del iPhone.
    */
   public static Map<TipoMovimiento, List<String>> etiquetasPorTipo(TipoMovimiento filtro) {
      return Arrays.stream(values())
            .filter(categoria -> filtro == null || categoria.tipo == filtro)
            .collect(Collectors.groupingBy(
                  Categoria::getTipo,
                  () -> new EnumMap<>(TipoMovimiento.class),
                  Collectors.mapping(Categoria::getEtiqueta, Collectors.toList())));
   }
}
