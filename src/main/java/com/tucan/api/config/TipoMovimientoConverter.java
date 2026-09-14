package com.tucan.api.config;

import com.tucan.api.model.TipoMovimiento;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Convierte el {@code ?tipo=...} de la URL con el mismo criterio que FIN-15 aplica
 * al cuerpo JSON.
 *
 * <p>Sin esto, Spring convierte los parametros con {@code Enum.valueOf}, que solo
 * acepta el nombre exacto de la constante: {@code ?tipo=GASTO} andaria y
 * {@code ?tipo=Gasto} daria 400. Como "Gasto" es justamente la etiqueta que usa la
 * hoja y que el atajo ya manda en el POST, tener dos criterios distintos segun la
 * puerta de entrada es una trampa esperando a alguien.
 */
@Component
public class TipoMovimientoConverter implements Converter<String, TipoMovimiento> {

   @Override
   public TipoMovimiento convert(String valor) {
      return TipoMovimiento.desde(valor);
   }
}
