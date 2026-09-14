package com.tucan.api.model;

import jakarta.validation.constraints.AssertTrue;
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
 * <p>Las anotaciones miran un campo a la vez; la coherencia entre {@code tipo} y
 * {@code categoria} necesita mirar dos, y por eso va aparte, en
 * {@link #isCategoriaCoherenteConElTipo()}.
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

   /**
    * Regla cruzada: un GASTO con categoria Salario tiene los dos campos validos
    * por separado y aun asi es un dato invalido, que ensuciaria el dashboard.
    *
    * <p>Vive en el modelo y no en el controlador para que se ejecute junto al
    * resto de las validaciones y produzca el mismo formato de error. Como cada
    * {@link Categoria} ya sabe a que tipo pertenece, la regla es una comparacion.
    *
    * <p>Con {@code tipo} o {@code categoria} nulos devuelve {@code true} a
    * proposito: de esos casos ya se encarga {@code @NotNull} con su propio
    * mensaje, y dos errores sobre lo mismo solo confunden en la pantalla del
    * iPhone.
    */
   @AssertTrue(message = "La categoria no corresponde al tipo de movimiento")
   public boolean isCategoriaCoherenteConElTipo() {
      return tipo == null || categoria == null || categoria.getTipo() == tipo;
   }
}
