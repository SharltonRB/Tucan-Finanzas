package com.tucan.api.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.tucan.api.config.AppProperties;
import com.tucan.api.model.Categoria;
import com.tucan.api.model.Medio;
import com.tucan.api.model.MovimientoRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Traduce un movimiento a una fila de la hoja y la agrega al final.
 *
 * <p>El orden de las siete celdas es el de las columnas A a G y no se negocia:
 * una fila corrida una posicion no rompe nada visible, solo hace que el dashboard
 * sume mal para siempre.
 */
@Service
public class MovimientoService {

   private static final Logger log = LoggerFactory.getLogger(MovimientoService.class);

   /** Se declara una sola vez: crear un formateador por peticion es desperdicio. */
   private static final DateTimeFormatter FORMATO_TIMESTAMP =
         DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

   /**
    * Columnas completas, sin numero de fila. Con un rango asi, {@code append}
    * busca sola la ultima fila con datos y escribe debajo, sin que haya que
    * llevar ningun contador.
    */
   private static final String COLUMNAS = "!A:G";

   /**
    * Con {@code USER_ENTERED} Sheets interpreta la fecha y el monto igual que si
    * los hubieras tecleado vos. Con {@code RAW} quedarian como texto, alineados a
    * la izquierda, y las formulas de fecha del dashboard dejarian de funcionar.
    */
   private static final String ENTRADA_COMO_SI_LA_ESCRIBIERAS = "USER_ENTERED";

   /** Agrega filas nuevas en vez de pisar lo que ya haya debajo. */
   private static final String INSERTAR_FILAS = "INSERT_ROWS";

   private final Sheets sheets;
   private final AppProperties propiedades;

   public MovimientoService(Sheets sheets, AppProperties propiedades) {
      this.sheets = sheets;
      this.propiedades = propiedades;
   }

   public void agregar(MovimientoRequest movimiento) throws IOException {
      ValueRange fila = new ValueRange().setValues(List.of(celdasDe(movimiento)));

      sheets.spreadsheets()
            .values()
            .append(propiedades.google().spreadsheetId(), rango(), fila)
            .setValueInputOption(ENTRADA_COMO_SI_LA_ESCRIBIERAS)
            .setInsertDataOption(INSERTAR_FILAS)
            .execute();

      log.info("movimiento_guardado tipo={} monto={}", movimiento.tipo(), movimiento.monto());
   }

   /**
    * Las siete celdas, en el orden exacto de la hoja. Los opcionales viajan como
    * cadena vacia y no como {@code null}, porque {@link List#of} rechaza los nulos
    * con un {@code NullPointerException}.
    */
   private List<Object> celdasDe(MovimientoRequest movimiento) {
      return List.of(
            movimiento.fecha().toString(),
            movimiento.tipo().getEtiqueta(),
            movimiento.monto(),
            etiquetaDe(movimiento.categoria()),
            textoDe(movimiento.descripcion()),
            ahora(),
            etiquetaDe(movimiento.medio()));
   }

   private String rango() {
      return propiedades.google().hoja() + COLUMNAS;
   }

   private String ahora() {
      return LocalDateTime.now(ZoneId.of(propiedades.zonaHoraria())).format(FORMATO_TIMESTAMP);
   }

   private static String etiquetaDe(Categoria categoria) {
      return categoria == null ? "" : categoria.getEtiqueta();
   }

   private static String etiquetaDe(Medio medio) {
      return medio == null ? "" : medio.getEtiqueta();
   }

   private static String textoDe(String valor) {
      return valor == null ? "" : valor;
   }
}
