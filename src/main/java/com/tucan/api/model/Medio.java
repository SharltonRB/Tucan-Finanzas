package com.tucan.api.model;

/**
 * Como se movio la plata. Aplica igual a ingresos y a gastos, por eso se llama
 * {@code Medio} y no {@code MedioPago}: en un ingreso no estas pagando nada.
 *
 * <p>La etiqueta es lo que se escribe en la columna G de la hoja. La marca de
 * efectivo vive aca porque el resumen mensual agrupa en efectivo / no efectivo,
 * y conviene que esa distincion no quede como comparaciones sueltas contra
 * constantes repartidas por el codigo.
 */
public enum Medio {

   EFECTIVO("Efectivo", true),
   SINPE_MOVIL("SINPE Movil", false),
   TARJETA("Tarjeta", false),
   TRANSFERENCIA("Transferencia", false),
   OTRO("Otro", false);

   private final String etiqueta;
   private final boolean efectivo;

   Medio(String etiqueta, boolean efectivo) {
      this.etiqueta = etiqueta;
      this.efectivo = efectivo;
   }

   public String getEtiqueta() {
      return etiqueta;
   }

   public boolean isEfectivo() {
      return efectivo;
   }
}
