package com.tucan.api.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El camino feliz de este bean no se puede probar sin una llave privada real, y
 * una llave privada no entra en el repositorio. Lo que si se prueba, y es lo que
 * de verdad duele en produccion, son los dos modos de fallar: que la variable no
 * sea base64, o que lo sea pero no contenga unas credenciales.
 *
 * <p>En los dos casos el mensaje tiene que nombrar la variable. Cuando la app no
 * arranque en Cloud Run a las once de la noche, eso es lo unico que vas a tener.
 */
class GoogleSheetsConfigTest {

   @Test
   @DisplayName("un base64 corrupto falla nombrando la variable de entorno")
   void base64Corrupto_fallaNombrandoLaVariable() {
      assertThatThrownBy(() -> GoogleSheetsConfig.credencialesDesde("!!!esto-no-es-base64!!!"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GOOGLE_CREDENTIALS_BASE64");
   }

   @Test
   @DisplayName("un base64 valido que no son credenciales falla nombrando la variable")
   void base64QueNoSonCredenciales_fallaNombrandoLaVariable() {
      // {"test":"fake"}, el mismo valor ficticio del application.yml de test.
      assertThatThrownBy(() -> GoogleSheetsConfig.credencialesDesde("eyJ0ZXN0IjoiZmFrZSJ9"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GOOGLE_CREDENTIALS_BASE64");
   }
}
