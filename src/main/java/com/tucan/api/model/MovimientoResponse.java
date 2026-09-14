package com.tucan.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que la API devuelve despues de guardar un movimiento, con HTTP 201.
 *
 * <p>Es el eco de lo guardado y no la peticion entera a proposito: al atajo le
 * alcanza con esto para armar la confirmacion en pantalla.
 */
public record MovimientoResponse(
      String mensaje,
      TipoMovimiento tipo,
      BigDecimal monto,
      LocalDate fecha) {

   private static final String CONFIRMACION = " guardado correctamente";

   /**
    * Arma la confirmacion de lo que se acaba de guardar: "Ingreso guardado
    * correctamente" o "Gasto guardado correctamente", segun el tipo.
    *
    * <p>El mensaje se arma aca y no en el controlador para que la puerta de entrada
    * HTTP se quede sin nada que decidir.
    */
   public static MovimientoResponse de(MovimientoRequest movimiento) {
      return new MovimientoResponse(
            movimiento.tipo().getEtiqueta() + CONFIRMACION,
            movimiento.tipo(),
            movimiento.monto(),
            movimiento.fecha());
   }
}
