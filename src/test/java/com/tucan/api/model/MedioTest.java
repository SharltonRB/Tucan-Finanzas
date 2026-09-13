package com.tucan.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MedioTest {

   @Test
   @DisplayName("hay exactamente cinco medios")
   void values_devuelveCincoConstantes() {
      assertThat(Medio.values()).hasSize(5);
   }

   @ParameterizedTest(name = "{0} -> \"{1}\", efectivo={2}")
   @DisplayName("cada medio tiene su etiqueta de la hoja y su marca de efectivo")
   @CsvSource({
      "EFECTIVO,       Efectivo,       true",
      "SINPE_MOVIL,    SINPE Movil,    false",
      "TARJETA,        Tarjeta,        false",
      "TRANSFERENCIA,  Transferencia,  false",
      "OTRO,           Otro,           false"
   })
   void cadaMedio_tieneEtiquetaYMarcaDeEfectivo(Medio medio, String etiqueta, boolean esEfectivo) {
      assertThat(medio.getEtiqueta()).isEqualTo(etiqueta);
      assertThat(medio.isEfectivo()).isEqualTo(esEfectivo);
   }

   @Test
   @DisplayName("EFECTIVO es el unico medio marcado como efectivo")
   void isEfectivo_soloEfectivoEstaMarcado() {
      assertThat(Medio.values())
            .filteredOn(Medio::isEfectivo)
            .containsExactly(Medio.EFECTIVO);
   }
}
