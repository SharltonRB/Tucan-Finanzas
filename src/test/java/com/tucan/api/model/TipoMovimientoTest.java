package com.tucan.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TipoMovimientoTest {

   @Test
   @DisplayName("hay exactamente dos tipos de movimiento")
   void values_devuelveDosConstantes() {
      assertThat(TipoMovimiento.values()).hasSize(2);
   }

   @Test
   @DisplayName("las etiquetas son las que espera la columna B de la hoja")
   void getEtiqueta_devuelveLaEtiquetaDeLaHoja() {
      assertThat(TipoMovimiento.INGRESO.getEtiqueta()).isEqualTo("Ingreso");
      assertThat(TipoMovimiento.GASTO.getEtiqueta()).isEqualTo("Gasto");
   }
}
