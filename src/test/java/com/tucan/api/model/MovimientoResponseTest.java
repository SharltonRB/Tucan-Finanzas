package com.tucan.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MovimientoResponseTest {

   @Test
   @DisplayName("se serializa con las cuatro claves del contrato y la fecha en ISO-8601")
   void seSerializaComoLoEsperaElContrato() throws Exception {
      // Jackson a secas escribe LocalDate como el array [2026,9,11]. Spring Boot
      // desactiva WRITE_DATES_AS_TIMESTAMPS por defecto, y por eso la API responde
      // "2026-09-11"; aca se replica esa configuracion para probar lo que de verdad
      // va a salir por HTTP.
      ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
      var respuesta = new MovimientoResponse(
            "Gasto guardado correctamente",
            TipoMovimiento.GASTO,
            new BigDecimal("4500"),
            LocalDate.of(2026, 9, 11));

      String json = mapper.writeValueAsString(respuesta);

      assertThat(json).isEqualTo("""
            {"mensaje":"Gasto guardado correctamente","tipo":"GASTO","monto":4500,"fecha":"2026-09-11"}""");
   }
}
