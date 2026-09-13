package com.tucan.api.model;

/**
 * Naturaleza de un movimiento: plata que entra o plata que sale.
 *
 * <p>La etiqueta es lo que se escribe en la columna B de la hoja, y tiene que
 * coincidir caracter por caracter con su lista desplegable.
 */
public enum TipoMovimiento {

   INGRESO("Ingreso"),
   GASTO("Gasto");

   private final String etiqueta;

   TipoMovimiento(String etiqueta) {
      this.etiqueta = etiqueta;
   }

   public String getEtiqueta() {
      return etiqueta;
   }
}
