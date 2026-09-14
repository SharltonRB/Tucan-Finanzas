package com.tucan.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Lo que la API acepta como texto para cada enum. El atajo manda la etiqueta sin
 * tildes, pero desde Bruno o curl alguien va a escribir "Alimentacion" con tilde,
 * en minusculas o con espacios de sobra, y eso tiene que resolver igual.
 */
class ResolucionDeEtiquetasTest {

   @Nested
   @DisplayName("Categoria")
   class CategoriaDesdeTexto {

      @ParameterizedTest(name = "\"{0}\" -> {1}")
      @DisplayName("acepta tildes, capitalizacion y espacios de sobra")
      @CsvSource({
         "Alimentacion,       ALIMENTACION",
         "Alimentación,       ALIMENTACION",
         "alimentacion,       ALIMENTACION",
         "ALIMENTACION,       ALIMENTACION",
         "'  TRANSPORTE  ',   TRANSPORTE",
         "Otros ingresos,     OTROS_INGRESOS",
         "Comida Chatarra,    COMIDA_CHATARRA",
         "Ahorro/Inversion,   AHORRO_INVERSION",
         "Ahorro/Inversión,   AHORRO_INVERSION",
         "Educación,          EDUCACION"
      })
      void seResuelveDesdeCualquierVarianteRazonable(String recibido, Categoria esperada) {
         assertThat(Categoria.desde(recibido)).isEqualTo(esperada);
      }

      @ParameterizedTest(name = "[{0}] -> null")
      @DisplayName("ausente o vacia devuelve null, sin reventar: ya la rechaza FIN-14")
      @NullAndEmptySource
      @ValueSource(strings = {"   "})
      void ausenteOVacia_devuelveNull(String recibido) {
         assertThat(Categoria.desde(recibido)).isNull();
      }

      @Test
      @DisplayName("una categoria inexistente falla diciendo que texto llego")
      void inexistente_fallaConElTextoRecibido() {
         assertThatThrownBy(() -> Categoria.desde("Criptomonedas"))
               .isInstanceOf(IllegalArgumentException.class)
               .hasMessageContaining("Criptomonedas");
      }
   }

   @Nested
   @DisplayName("Medio")
   class MedioDesdeTexto {

      @ParameterizedTest(name = "\"{0}\" -> SINPE_MOVIL")
      @DisplayName("las tres variantes de SINPE resuelven al mismo medio")
      @ValueSource(strings = {"SINPE Movil", "SINPE Móvil", "sinpe movil", "SINPE_MOVIL"})
      void variantesDeSinpe_resuelvenAlMismoValor(String recibido) {
         assertThat(Medio.desde(recibido)).isEqualTo(Medio.SINPE_MOVIL);
      }

      @Test
      @DisplayName("un medio inexistente falla diciendo que texto llego")
      void inexistente_fallaConElTextoRecibido() {
         assertThatThrownBy(() -> Medio.desde("Bitcoin"))
               .isInstanceOf(IllegalArgumentException.class)
               .hasMessageContaining("Bitcoin");
      }
   }

   @Nested
   @DisplayName("TipoMovimiento")
   class TipoDesdeTexto {

      @ParameterizedTest(name = "\"{0}\" -> GASTO")
      @DisplayName("acepta tanto la constante como la etiqueta de la hoja")
      @ValueSource(strings = {"GASTO", "Gasto", "gasto", "  Gasto  "})
      void variantesDeGasto_resuelvenAlMismoTipo(String recibido) {
         assertThat(TipoMovimiento.desde(recibido)).isEqualTo(TipoMovimiento.GASTO);
      }

      @Test
      @DisplayName("un tipo inexistente falla diciendo que texto llego")
      void inexistente_fallaConElTextoRecibido() {
         assertThatThrownBy(() -> TipoMovimiento.desde("Ahorro"))
               .isInstanceOf(IllegalArgumentException.class)
               .hasMessageContaining("Ahorro");
      }
   }
}
