package com.tucan.api.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que llega por HTTP al registrar un movimiento.
 *
 * <p>Es la frontera del sistema: aca se detienen los datos invalidos, antes de
 * tocar la logica y muchisimo antes de llegar a la hoja.
 *
 * <p>Los mensajes van en espanol y dicen que hacer, porque el unico que los va a
 * leer es el atajo en una pantalla de iPhone.
 *
 * <p>El monto es {@link BigDecimal} y no {@code double}: los flotantes no
 * representan exactos los decimales en base 10, y con dinero ese error se acumula
 * en descuadres que despues nadie sabe explicar.
 *
 * <p>La coherencia entre {@code tipo} y {@code categoria} no se valida aca: cada
 * anotacion mira un campo a la vez. Esa regla cruzada llega en FIN-14.
 */
public record MovimientoRequest(

      @NotNull(message = "El tipo es obligatorio: INGRESO o GASTO")
      TipoMovimiento tipo,

      @NotNull(message = "El monto es obligatorio")
      @DecimalMin(value = "0", inclusive = false, message = "El monto tiene que ser mayor que cero")
      BigDecimal monto,

      @NotNull(message = "La fecha es obligatoria, con formato aaaa-mm-dd")
      LocalDate fecha,

      @NotNull(message = "La categoria es obligatoria, tanto en ingresos como en gastos")
      Categoria categoria,

      @Size(max = 200, message = "La descripcion no puede pasar de 200 caracteres")
      String descripcion,

      @NotNull(message = "El medio es obligatorio, tanto en ingresos como en gastos")
      Medio medio) {
}
