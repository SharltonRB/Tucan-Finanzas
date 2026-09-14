package com.tucan.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;

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

   /**
    * Resuelve el texto que llega en el JSON. El atajo manda "GASTO", pero la
    * etiqueta de la hoja es "Gasto" y las dos tienen que servir.
    */
   @JsonCreator
   public static TipoMovimiento desde(String valor) {
      return Etiquetas.resolver(values(), valor, "Tipo de movimiento no reconocido");
   }
}
