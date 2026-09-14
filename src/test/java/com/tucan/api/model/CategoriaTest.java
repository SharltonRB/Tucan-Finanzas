package com.tucan.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CategoriaTest {

   @Test
   @DisplayName("hay exactamente trece categorias")
   void values_devuelveTreceConstantes() {
      assertThat(Categoria.values()).hasSize(13);
   }

   @ParameterizedTest(name = "{0} -> \"{1}\" ({2})")
   @DisplayName("cada categoria tiene su etiqueta de la hoja y su tipo de movimiento")
   @CsvSource({
      "SALARIO,          Salario,           INGRESO",
      "OTROS_INGRESOS,   Otros ingresos,    INGRESO",
      "ALIMENTACION,     Alimentacion,      GASTO",
      "COMIDA_CHATARRA,  Comida Chatarra,   GASTO",
      "TRANSPORTE,       Transporte,        GASTO",
      "ALQUILER,         Alquiler,          GASTO",
      "SERVICIOS,        Servicios,         GASTO",
      "SALUD,            Salud,             GASTO",
      "EDUCACION,        Educacion,         GASTO",
      "ENTRETENIMIENTO,  Entretenimiento,   GASTO",
      "ELECTRONICO,      Electronico,       GASTO",
      "AHORRO_INVERSION, Ahorro/Inversion,  GASTO",
      "OTROS_GASTOS,     Otros gastos,      GASTO"
   })
   void cadaCategoria_tieneEtiquetaYTipo(Categoria categoria, String etiqueta, TipoMovimiento tipo) {
      assertThat(categoria.getEtiqueta()).isEqualTo(etiqueta);
      assertThat(categoria.getTipo()).isEqualTo(tipo);
   }

   @Test
   @DisplayName("el reparto es dos categorias de ingreso y once de gasto")
   void getTipo_repartoDosIngresosOnceGastos() {
      assertThat(Categoria.values())
            .filteredOn(categoria -> categoria.getTipo() == TipoMovimiento.INGRESO)
            .containsExactly(Categoria.SALARIO, Categoria.OTROS_INGRESOS);

      assertThat(Categoria.values())
            .filteredOn(categoria -> categoria.getTipo() == TipoMovimiento.GASTO)
            .hasSize(11);
   }

   @Test
   @DisplayName("AHORRO_INVERSION es GASTO: como ingreso duplicaria el dinero del salario")
   void getTipo_ahorroInversionEsGasto() {
      assertThat(Categoria.AHORRO_INVERSION.getTipo()).isEqualTo(TipoMovimiento.GASTO);
   }

   @Test
   @DisplayName("ninguna etiqueta lleva tildes, igual que la lista desplegable de la hoja")
   void getEtiqueta_sinTildes() {
      assertThat(Categoria.values())
            .extracting(Categoria::getEtiqueta)
            .allSatisfy(etiqueta -> assertThat(etiqueta).doesNotContainPattern("[áéíóúÁÉÍÓÚñÑ]"));
   }

   @Test
   @DisplayName("sin filtro devuelve las dos ramas, que son los dos menus del atajo")
   void etiquetasPorTipo_sinFiltro_devuelveLasDosRamas() {
      Map<TipoMovimiento, List<String>> menus = Categoria.etiquetasPorTipo(null);

      assertThat(menus).containsOnlyKeys(TipoMovimiento.INGRESO, TipoMovimiento.GASTO);
      assertThat(menus.get(TipoMovimiento.INGRESO)).hasSize(2);
      assertThat(menus.get(TipoMovimiento.GASTO)).hasSize(11);
   }

   @Test
   @DisplayName("con filtro devuelve solo la rama pedida")
   void etiquetasPorTipo_conFiltro_devuelveSoloEsaRama() {
      assertThat(Categoria.etiquetasPorTipo(TipoMovimiento.INGRESO))
            .containsOnlyKeys(TipoMovimiento.INGRESO);

      assertThat(Categoria.etiquetasPorTipo(TipoMovimiento.GASTO))
            .containsOnlyKeys(TipoMovimiento.GASTO);
   }

   @Test
   @DisplayName("devuelve las etiquetas de la hoja, no el nombre de las constantes")
   void etiquetasPorTipo_devuelveEtiquetas() {
      assertThat(Categoria.etiquetasPorTipo(TipoMovimiento.INGRESO).get(TipoMovimiento.INGRESO))
            .containsExactly("Salario", "Otros ingresos");
   }

   @Test
   @DisplayName("el orden es el de declaracion: es el orden en que se ve el menu del atajo")
   void etiquetasPorTipo_conservaElOrdenDeDeclaracion() {
      assertThat(Categoria.etiquetasPorTipo(TipoMovimiento.GASTO).get(TipoMovimiento.GASTO))
            .startsWith("Alimentacion", "Comida Chatarra", "Transporte")
            .endsWith("Ahorro/Inversion", "Otros gastos");
   }
}
