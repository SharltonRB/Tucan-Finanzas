package com.tucan.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MovimientoRequestTest {

   private static ValidatorFactory factory;
   private static Validator validator;

   @BeforeAll
   static void abrirValidador() {
      factory = Validation.buildDefaultValidatorFactory();
      validator = factory.getValidator();
   }

   @AfterAll
   static void cerrarValidador() {
      factory.close();
   }

   @Test
   @DisplayName("un gasto completo no tiene ninguna violacion")
   void gastoCompleto_esValido() {
      var peticion = gasto("4500", "Almuerzo");

      assertThat(validator.validate(peticion)).isEmpty();
   }

   @Test
   @DisplayName("la descripcion es opcional: puede venir nula")
   void descripcionNula_esValida() {
      var peticion = gasto("4500", null);

      assertThat(validator.validate(peticion)).isEmpty();
   }

   @ParameterizedTest(name = "monto {0} es invalido")
   @DisplayName("el monto tiene que ser mayor que cero")
   @ValueSource(strings = {"0", "0.00", "-1", "-4500.50"})
   void montoNoPositivo_esInvalido(String monto) {
      var peticion = gasto(monto, "Almuerzo");

      assertThat(propiedadesConError(peticion)).containsExactly("monto");
   }

   @Test
   @DisplayName("el monto acepta un decimal chico, apenas por encima de cero")
   void montoApenasPositivo_esValido() {
      var peticion = gasto("0.01", "Almuerzo");

      assertThat(validator.validate(peticion)).isEmpty();
   }

   @Test
   @DisplayName("tipo, monto, fecha, categoria y medio son obligatorios")
   void camposObligatoriosNulos_sonInvalidos() {
      var peticion = new MovimientoRequest(null, null, null, null, "Almuerzo", null);

      assertThat(propiedadesConError(peticion))
            .containsExactlyInAnyOrder("tipo", "monto", "fecha", "categoria", "medio");
   }

   @Test
   @DisplayName("la descripcion admite 200 caracteres pero no 201")
   void descripcionDemasiadoLarga_esInvalida() {
      assertThat(validator.validate(gasto("4500", "a".repeat(200)))).isEmpty();

      assertThat(propiedadesConError(gasto("4500", "a".repeat(201))))
            .containsExactly("descripcion");
   }

   @Test
   @DisplayName("todos los mensajes de validacion estan en espanol, no son los de la libreria")
   void mensajes_estanEnEspanol() {
      var peticion = new MovimientoRequest(null, null, null, null, "a".repeat(201), null);

      assertThat(validator.validate(peticion))
            .isNotEmpty()
            .allSatisfy(violacion -> assertThat(violacion.getMessage())
                  .doesNotStartWith("{")
                  .doesNotContain("must", "debe ser"));
   }

   @Test
   @DisplayName("Jackson deserializa el cuerpo del contrato, con la fecha en ISO-8601")
   void jackson_deserializaElCuerpoDelContrato() throws Exception {
      ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
      String json = """
            {
              "tipo": "GASTO",
              "monto": 4500,
              "fecha": "2026-09-11",
              "categoria": "ALIMENTACION",
              "descripcion": "Almuerzo",
              "medio": "EFECTIVO"
            }
            """;

      var peticion = mapper.readValue(json, MovimientoRequest.class);

      assertThat(peticion.tipo()).isEqualTo(TipoMovimiento.GASTO);
      assertThat(peticion.monto()).isEqualByComparingTo("4500");
      assertThat(peticion.fecha()).isEqualTo(LocalDate.of(2026, 9, 11));
      assertThat(peticion.categoria()).isEqualTo(Categoria.ALIMENTACION);
      assertThat(peticion.descripcion()).isEqualTo("Almuerzo");
      assertThat(peticion.medio()).isEqualTo(Medio.EFECTIVO);
   }

   private static MovimientoRequest gasto(String monto, String descripcion) {
      return new MovimientoRequest(
            TipoMovimiento.GASTO,
            new BigDecimal(monto),
            LocalDate.of(2026, 9, 11),
            Categoria.ALIMENTACION,
            descripcion,
            Medio.EFECTIVO);
   }

   private static Set<String> propiedadesConError(MovimientoRequest peticion) {
      return validator.validate(peticion).stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(Object::toString)
            .collect(Collectors.toSet());
   }
}
