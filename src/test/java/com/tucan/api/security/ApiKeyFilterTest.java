package com.tucan.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.api.services.sheets.v4.Sheets;
import com.tucan.api.service.MovimientoService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba la cadena de filtros de verdad y no el filtro suelto: lo que importa es
 * que ninguna peticion llegue al controlador sin pasar por el, y eso solo se ve
 * con el contexto entero levantado.
 *
 * <p>La API key de estas pruebas es la ficticia del application.yml de test, no la
 * real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyFilterTest {

   private static final String HEADER = "X-API-Key";
   private static final String KEY_CORRECTA = "test-api-key";

   private static final String GASTO_VALIDO = """
         {"tipo":"Gasto","monto":4500,"fecha":"2026-09-11",
          "categoria":"Alimentacion","descripcion":"Almuerzo","medio":"SINPE Movil"}""";

   @Autowired private MockMvc mockMvc;

   /**
    * El cliente de Sheets se sustituye por un mock: el application.yml de test trae
    * credenciales ficticias y el bean de verdad no podria construirse.
    */
   @MockBean private Sheets sheets;

   @MockBean private MovimientoService servicio;

   @Test
   @DisplayName("GET /api/ping funciona sin header: hay que poder ver si la API vive sin la key")
   void ping_sinHeader_funciona() throws Exception {
      mockMvc.perform(get("/api/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("pong"));
   }

   @Test
   @DisplayName("GET /api/categorias sin header responde 401")
   void categorias_sinHeader_responde401() throws Exception {
      mockMvc.perform(get("/api/categorias")).andExpect(status().isUnauthorized());
   }

   @Test
   @DisplayName("una key incorrecta responde 401")
   void categorias_keyIncorrecta_responde401() throws Exception {
      mockMvc.perform(get("/api/categorias").header(HEADER, "key-equivocada"))
            .andExpect(status().isUnauthorized());
   }

   @Test
   @DisplayName("POST /api/movimientos con key incorrecta responde 401 y no toca la hoja")
   void movimientos_keyIncorrecta_responde401() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .header(HEADER, "key-equivocada")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(GASTO_VALIDO))
            .andExpect(status().isUnauthorized());

      verify(servicio, never()).agregar(any());
   }

   @Test
   @DisplayName("una key vacia tambien responde 401")
   void categorias_keyVacia_responde401() throws Exception {
      mockMvc.perform(get("/api/categorias").header(HEADER, ""))
            .andExpect(status().isUnauthorized());
   }

   @Test
   @DisplayName("el cuerpo del 401 es JSON en UTF-8 con un mensaje que se entiende en el iPhone")
   void cuerpoDel401_esJsonLegible() throws Exception {
      mockMvc.perform(get("/api/categorias"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().encoding(StandardCharsets.UTF_8))
            .andExpect(jsonPath("$.error").isNotEmpty());
   }

   @Test
   @DisplayName("con la key correcta, GET /api/categorias sigue funcionando igual")
   void categorias_keyCorrecta_pasa() throws Exception {
      mockMvc.perform(get("/api/categorias").header(HEADER, KEY_CORRECTA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.INGRESO[0]").value("Salario"));
   }

   @Test
   @DisplayName("con la key correcta, POST /api/movimientos sigue respondiendo 201")
   void movimientos_keyCorrecta_responde201() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .header(HEADER, KEY_CORRECTA)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(GASTO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.mensaje").value("Gasto guardado correctamente"));

      verify(servicio).agregar(any());
   }
}
