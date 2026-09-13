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
}
