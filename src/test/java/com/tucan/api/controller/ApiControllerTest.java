package com.tucan.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tucan.api.model.Categoria;
import com.tucan.api.model.Medio;
import com.tucan.api.model.MovimientoRequest;
import com.tucan.api.model.TipoMovimiento;
import com.tucan.api.security.ApiKeyFilter;
import com.tucan.api.service.MovimientoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba la capa web sola: el servicio va mockeado, asi que ninguna de estas
 * pruebas toca la hoja de calculo.
 *
 * <p>Lo que mas importa aca son los casos de 400. Si el {@code @Valid} del
 * controlador se cae, todo sigue compilando y arrancando, y la API acepta basura
 * sin dar ninguna senal; los tests de monto negativo y de categoria incoherente
 * son el unico aviso.
 *
 * <p>El {@link ApiKeyFilter} se excluye a proposito: aca se prueba el controlador,
 * y la API key tiene su propio test de extremo a extremo. Sin la exclusion,
 * {@code @WebMvcTest} intenta construir el filtro y se cae, porque el slice web no
 * levanta las {@code @ConfigurationProperties}.
 */
@WebMvcTest(
      controllers = ApiController.class,
      excludeFilters =
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
class ApiControllerTest {

   private static final String GASTO_VALIDO = """
         {"tipo":"Gasto","monto":4500,"fecha":"2026-09-11",
          "categoria":"Alimentacion","descripcion":"Almuerzo","medio":"SINPE Movil"}""";

   @Autowired private MockMvc mockMvc;

   @MockBean private MovimientoService servicio;

   @Test
   @DisplayName("GET /api/ping responde pong, para saber desde afuera si la API esta viva")
   void ping_respondePong() throws Exception {
      mockMvc.perform(get("/api/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("pong"));
   }

   @Test
   @DisplayName("GET /api/categorias devuelve las dos ramas del menu en una sola peticion")
   void categorias_sinFiltro_devuelveLasDosRamas() throws Exception {
      mockMvc.perform(get("/api/categorias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.INGRESO", hasSize(2)))
            .andExpect(jsonPath("$.GASTO", hasSize(11)))
            .andExpect(jsonPath("$.INGRESO[0]").value("Salario"));
   }

   @Test
   @DisplayName("GET /api/categorias?tipo=INGRESO devuelve solo las de ingreso")
   void categorias_filtradasPorIngreso() throws Exception {
      mockMvc.perform(get("/api/categorias").param("tipo", "INGRESO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.INGRESO", hasSize(2)))
            .andExpect(jsonPath("$.GASTO").doesNotExist());
   }

   @Test
   @DisplayName("GET /api/categorias?tipo=GASTO devuelve solo las de gasto")
   void categorias_filtradasPorGasto() throws Exception {
      mockMvc.perform(get("/api/categorias").param("tipo", "GASTO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.GASTO", hasSize(11)))
            .andExpect(jsonPath("$.INGRESO").doesNotExist());
   }

   @Test
   @DisplayName("el filtro tambien acepta la etiqueta de la hoja, igual que el cuerpo del POST")
   void categorias_filtroAceptaLaEtiqueta() throws Exception {
      mockMvc.perform(get("/api/categorias").param("tipo", "Gasto"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.GASTO", hasSize(11)));
   }

   @Test
   @DisplayName("POST /api/movimientos con datos validos responde 201 y delega en el servicio")
   void movimientos_datosValidos_responde201() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(GASTO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.mensaje").value("Gasto guardado correctamente"))
            .andExpect(jsonPath("$.tipo").value("GASTO"))
            .andExpect(jsonPath("$.monto").value(4500))
            .andExpect(jsonPath("$.fecha").value("2026-09-11"));

      var capturador = ArgumentCaptor.forClass(MovimientoRequest.class);
      verify(servicio).agregar(capturador.capture());

      MovimientoRequest guardado = capturador.getValue();
      assertThat(guardado.tipo()).isEqualTo(TipoMovimiento.GASTO);
      assertThat(guardado.categoria()).isEqualTo(Categoria.ALIMENTACION);
      assertThat(guardado.medio()).isEqualTo(Medio.SINPE_MOVIL);
      assertThat(guardado.monto()).isEqualByComparingTo(new BigDecimal("4500"));
      assertThat(guardado.fecha()).isEqualTo(LocalDate.of(2026, 9, 11));
   }

   @Test
   @DisplayName("un ingreso recibe su propio mensaje de confirmacion")
   void movimientos_ingreso_mensajePropio() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                        {"tipo":"Ingreso","monto":650000,"fecha":"2026-09-15",
                         "categoria":"Salario","medio":"SINPE Movil"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.mensaje").value("Ingreso guardado correctamente"));
   }

   @Test
   @DisplayName("un monto negativo responde 400 y no llega a la hoja: ahi se ve que el @Valid esta")
   void movimientos_montoNegativo_responde400() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                        {"tipo":"Gasto","monto":-4500,"fecha":"2026-09-11",
                         "categoria":"Alimentacion","medio":"Efectivo"}"""))
            .andExpect(status().isBadRequest());

      verify(servicio, never()).agregar(any());
   }

   @Test
   @DisplayName("un gasto con categoria de ingreso responde 400: la regla cruzada de FIN-14 corre")
   void movimientos_categoriaIncoherente_responde400() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                        {"tipo":"Gasto","monto":4500,"fecha":"2026-09-11",
                         "categoria":"Salario","medio":"Efectivo"}"""))
            .andExpect(status().isBadRequest());

      verify(servicio, never()).agregar(any());
   }

   @Test
   @DisplayName("un movimiento sin categoria responde 400")
   void movimientos_sinCategoria_responde400() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                        {"tipo":"Gasto","monto":4500,"fecha":"2026-09-11","medio":"Efectivo"}"""))
            .andExpect(status().isBadRequest());

      verify(servicio, never()).agregar(any());
   }
}
