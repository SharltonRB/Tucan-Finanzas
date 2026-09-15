package com.tucan.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.tucan.api.config.AppProperties;
import com.tucan.api.model.Categoria;
import com.tucan.api.model.Medio;
import com.tucan.api.model.MovimientoRequest;
import com.tucan.api.model.TipoMovimiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Lo que de verdad importa de este servicio es el orden de las celdas: la hoja
 * tiene siete columnas fijas y una fila corrida una posicion arruina todas las
 * formulas del dashboard sin avisar.
 */
class MovimientoServiceTest {

   private static final String SPREADSHEET_ID = "test-spreadsheet-id";
   private static final String HOJA = "Movimientos";

   private Sheets sheets;
   private Sheets.Spreadsheets.Values.Append append;
   private MovimientoService servicio;

   @BeforeEach
   void prepararElCliente() throws Exception {
      sheets = mock(Sheets.class, RETURNS_DEEP_STUBS);
      append = mock(Sheets.Spreadsheets.Values.Append.class);

      when(sheets.spreadsheets().values().append(any(), any(), any())).thenReturn(append);
      when(append.setValueInputOption(any())).thenReturn(append);
      when(append.setInsertDataOption(any())).thenReturn(append);
      when(append.execute()).thenReturn(new AppendValuesResponse());

      var google = new AppProperties.Google("credenciales-en-base64", SPREADSHEET_ID, HOJA);
      var propiedades = new AppProperties("test-api-key", "America/Costa_Rica", google);
      servicio = new MovimientoService(sheets, propiedades);
   }

   @Test
   @DisplayName("un gasto se escribe en las siete columnas, en el orden de la hoja")
   void gasto_seEscribeEnElOrdenDeLaHoja() throws Exception {
      servicio.agregar(gasto());

      List<Object> fila = filaEnviada();
      assertThat(fila).hasSize(7);
      assertThat(fila.get(0)).isEqualTo("2026-09-11");
      assertThat(fila.get(1)).isEqualTo("Gasto");
      assertThat(fila.get(2)).isEqualTo(new BigDecimal("4500"));
      assertThat(fila.get(3)).isEqualTo("Alimentacion");
      assertThat(fila.get(4)).isEqualTo("Almuerzo");
      assertThat(fila.get(5)).asString().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
      assertThat(fila.get(6)).isEqualTo("Efectivo");
   }

   @Test
   @DisplayName("las columnas B, D y G llevan la etiqueta, no el nombre de la constante")
   void ingreso_escribeLasEtiquetasYNoLasConstantes() throws Exception {
      servicio.agregar(ingresoSinDescripcion());

      List<Object> fila = filaEnviada();
      assertThat(fila.get(1)).isEqualTo("Ingreso");
      assertThat(fila.get(3)).isEqualTo("Salario");
      assertThat(fila.get(6)).isEqualTo("SINPE Movil");
   }

   @Test
   @DisplayName("una descripcion nula se escribe como celda vacia, sin reventar")
   void descripcionNula_seEscribeComoCeldaVacia() throws Exception {
      servicio.agregar(ingresoSinDescripcion());

      assertThat(filaEnviada().get(4)).isEqualTo("");
   }

   @Test
   @DisplayName("el spreadsheet y la hoja salen de la configuracion, no hardcodeados")
   void usaElSpreadsheetYLaHojaDeLaConfiguracion() throws Exception {
      servicio.agregar(gasto());

      verify(sheets.spreadsheets().values())
            .append(eq(SPREADSHEET_ID), eq("Movimientos!A:G"), any(ValueRange.class));
   }

   @Test
   @DisplayName("deja que Sheets interprete fecha y monto, y escribe sin insertar filas")
   void usaUserEnteredYOverwrite() throws Exception {
      servicio.agregar(gasto());

      verify(append).setValueInputOption("USER_ENTERED");
      verify(append).setInsertDataOption("OVERWRITE");
   }

   private static MovimientoRequest gasto() {
      return new MovimientoRequest(
            TipoMovimiento.GASTO,
            new BigDecimal("4500"),
            LocalDate.of(2026, 9, 11),
            Categoria.ALIMENTACION,
            "Almuerzo",
            Medio.EFECTIVO);
   }

   private static MovimientoRequest ingresoSinDescripcion() {
      return new MovimientoRequest(
            TipoMovimiento.INGRESO,
            new BigDecimal("650000"),
            LocalDate.of(2026, 9, 15),
            Categoria.SALARIO,
            null,
            Medio.SINPE_MOVIL);
   }

   private List<Object> filaEnviada() throws Exception {
      var capturador = ArgumentCaptor.forClass(ValueRange.class);
      verify(sheets.spreadsheets().values()).append(any(), any(), capturador.capture());
      return capturador.getValue().getValues().get(0);
   }
}
