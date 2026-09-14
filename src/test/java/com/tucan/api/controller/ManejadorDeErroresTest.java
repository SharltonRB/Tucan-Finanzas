package com.tucan.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tucan.api.security.ApiKeyFilter;
import com.tucan.api.service.MovimientoService;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba lo que ve el iPhone cuando algo sale mal, que es el unico lugar donde
 * estos mensajes se leen.
 *
 * <p>Cada caso mira el cuerpo y no solo el status: un 400 con el texto por defecto
 * de Spring, en ingles y con el nombre de una clase interna adentro, cumple el
 * status y aun asi no sirve de nada en una pantalla de telefono.
 *
 * <p>El {@link ApiKeyFilter} se excluye por el mismo motivo que en
 * {@link ApiControllerTest}: el slice web no levanta las
 * {@code @ConfigurationProperties} que el filtro necesita.
 */
@WebMvcTest(
      controllers = ApiController.class,
      excludeFilters =
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
class ManejadorDeErroresTest {

   /** Un secreto de mentira, para comprobar que el 500 no lo deja salir. */
   private static final String DETALLE_INTERNO = "credencial-vencida-en-el-servidor";

   private static final String GASTO_VALIDO = """
         {"tipo":"Gasto","monto":4500,"fecha":"2026-09-11",
          "categoria":"Alimentacion","descripcion":"Almuerzo","medio":"Efectivo"}""";

   @Autowired private MockMvc mockMvc;

   @MockBean private MovimientoService servicio;

   private ListAppender<ILoggingEvent> registro;

   /**
    * Engancha un appender al logger del manejador. Que el 500 quede registrado es
    * parte del trato: si el detalle no sale hacia el cliente, tiene que quedar en
    * algun lado, o el error se pierde del todo.
    */
   @BeforeEach
   void engancharElLog() {
      registro = new ListAppender<>();
      registro.start();
      logger().addAppender(registro);
   }

   @AfterEach
   void soltarElLog() {
      logger().detachAppender(registro);
   }

   private static ch.qos.logback.classic.Logger logger() {
      return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ManejadorDeErrores.class);
   }

   @Test
   @DisplayName("un gasto sin categoria responde 400 y el cuerpo nombra el campo que falta")
   void sinCategoria_nombraElCampo() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                        {"tipo":"Gasto","monto":4500,"fecha":"2026-09-11","medio":"Efectivo"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").isNotEmpty())
            .andExpect(jsonPath("$.campos.categoria").value(containsString("obligatoria")));
   }

   @Test
   @DisplayName("el monto invalido llega con su propio mensaje, el mismo que dice el modelo")
   void montoNegativo_explicaElPorque() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                        {"tipo":"Gasto","monto":-4500,"fecha":"2026-09-11",
                         "categoria":"Alimentacion","medio":"Efectivo"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.monto").value(containsString("mayor que cero")));
   }

   @Test
   @DisplayName("la regla cruzada de FIN-14 tambien sale como campo, no como texto suelto")
   void categoriaIncoherente_saleComoCampo() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                        {"tipo":"Gasto","monto":4500,"fecha":"2026-09-11",
                         "categoria":"Salario","medio":"Efectivo"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.categoriaCoherenteConElTipo")
                  .value(containsString("no corresponde")));
   }

   @Test
   @DisplayName("un JSON con una llave sin cerrar responde 400, no 500")
   void jsonRoto_responde400() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"tipo\":\"Gasto\",\"monto\":4500"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").isNotEmpty())
            .andExpect(jsonPath("$.detalle").value(containsString("llaves")))
            // Jackson explica esto con "Unexpected end-of-input ... StreamReadFeature
            // .INCLUDE_SOURCE_IN_LOCATION", en ingles y nombrando sus propias clases.
            // Ese texto no tiene por que llegar a una pantalla de iPhone.
            .andExpect(content().string(not(containsString("StreamReadFeature"))))
            .andExpect(content().string(not(containsString("Unexpected end-of-input"))));
   }

   @Test
   @DisplayName("una fecha imposible responde 400 diciendo el formato, no el ingles de java.time")
   void fechaImposible_diceElFormatoEsperado() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                        {"tipo":"Gasto","monto":4500,"fecha":"2026-13-45",
                         "categoria":"Alimentacion","medio":"Efectivo"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalle").value(containsString("aaaa-mm-dd")))
            // java.time lo explica con "Invalid value for MonthOfYear (valid values
            // 1 - 12): 13", que en un iPhone no le dice nada a nadie.
            .andExpect(content().string(not(containsString("MonthOfYear"))));
   }

   @Test
   @DisplayName("una categoria inexistente responde 400 y el cuerpo repite el valor recibido")
   void categoriaInexistente_mencionaElValor() throws Exception {
      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("""
                        {"tipo":"Gasto","monto":4500,"fecha":"2026-09-11",
                         "categoria":"Volar","medio":"Efectivo"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalle").value(containsString("Volar")));
   }

   @Test
   @DisplayName("un ?tipo= que no existe responde 400 y repite lo que llego")
   void tipoInvalidoEnLaUrl_responde400() throws Exception {
      mockMvc.perform(get("/api/categorias").param("tipo", "Volar"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalle").value(containsString("Volar")));
   }

   @Test
   @DisplayName("si Google falla, la culpa no es nuestra: 502 con el detalle del fallo")
   void falloDeGoogle_responde502() throws Exception {
      doThrow(new IOException("la hoja no existe")).when(servicio).agregar(any());

      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(GASTO_VALIDO))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error").isNotEmpty())
            .andExpect(jsonPath("$.detalle").value(containsString("la hoja no existe")));
   }

   @Test
   @DisplayName("el fallo de Google queda en el log del servidor")
   void falloDeGoogle_quedaEnElLog() throws Exception {
      doThrow(new IOException("la hoja no existe")).when(servicio).agregar(any());

      mockMvc.perform(post("/api/movimientos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(GASTO_VALIDO));

      assertThat(registro.list)
            .anySatisfy(evento -> assertThat(evento.getLevel()).isEqualTo(Level.ERROR));
   }

   @Test
   @DisplayName("un error inesperado responde 500 y no deja salir el mensaje interno")
   void errorInesperado_noFiltraElMensaje() throws Exception {
      doThrow(new IllegalStateException(DETALLE_INTERNO)).when(servicio).agregar(any());

      mockMvc.perform(post("/api/movimientos")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(GASTO_VALIDO))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").isNotEmpty())
            .andExpect(jsonPath("$.detalle").doesNotExist())
            .andExpect(content().string(not(containsString(DETALLE_INTERNO))));
   }

   @Test
   @DisplayName("una ruta que no existe sigue respondiendo 404, no 500")
   void rutaInexistente_sigueSiendo404() throws Exception {
      mockMvc.perform(get("/api/esta-ruta-no-existe")).andExpect(status().isNotFound());
   }

   @Test
   @DisplayName("ese mismo error inesperado si queda completo en el log del servidor")
   void errorInesperado_quedaEnElLog() throws Exception {
      doThrow(new IllegalStateException(DETALLE_INTERNO)).when(servicio).agregar(any());

      mockMvc.perform(post("/api/movimientos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(GASTO_VALIDO));

      assertThat(registro.list)
            .anySatisfy(evento -> {
               assertThat(evento.getLevel()).isEqualTo(Level.ERROR);
               assertThat(evento.getThrowableProxy().getMessage()).contains(DETALLE_INTERNO);
            });
   }
}
