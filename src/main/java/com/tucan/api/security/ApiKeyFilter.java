package com.tucan.api.security;

import com.tucan.api.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Deja pasar solo las peticiones que traen la API key correcta.
 *
 * <p>Un filtro corre antes que el controlador y antes de que Spring deserialice
 * nada, asi que una peticion sin key no llega a tocar el modelo ni la hoja. Para un
 * solo usuario y un solo cliente esto resuelve lo mismo que Spring Security en
 * treinta lineas.
 *
 * <p>{@code /api/ping} queda abierto a proposito: sirve para ver desde afuera si la
 * API esta despierta sin exponer la key en ningun lado.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

   private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

   private static final String HEADER = "X-API-Key";

   /** La unica ruta sin llave. Ver {@link #shouldNotFilter}. */
   private static final String RUTA_ABIERTA = "/api/ping";

   /**
    * El cuerpo del error se escribe a mano y no con Jackson: a esta altura de la
    * cadena todavia no hay controlador, y armarlo aca sale mas simple que meter un
    * ObjectMapper en el filtro.
    */
   private static final String CUERPO_401 =
         "{\"error\":\"API key invalida o ausente. Revisa el header X-API-Key.\"}";

   private final String apiKeyEsperada;

   public ApiKeyFilter(AppProperties propiedades) {
      this.apiKeyEsperada = propiedades.apiKey();
   }

   /**
    * Dejar {@code /api/ping} fuera del filtro es mas claro que meter un {@code if}
    * adentro: asi la regla de que se protege y que no queda en un solo metodo.
    */
   @Override
   protected boolean shouldNotFilter(HttpServletRequest peticion) {
      return RUTA_ABIERTA.equals(peticion.getRequestURI());
   }

   @Override
   protected void doFilterInternal(
         HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
         throws ServletException, IOException {

      if (!esValida(peticion.getHeader(HEADER))) {
         responderNoAutorizado(peticion, respuesta);
         // Sin este return la peticion seguiria su camino igual, con el 401 ya
         // escrito y el movimiento guardado. Es el bug sutil de los filtros.
         return;
      }

      cadena.doFilter(peticion, respuesta);
   }

   /**
    * Compara con {@link MessageDigest#isEqual} y no con {@code equals}. El
    * {@code equals} de String corta en el primer caracter distinto, y alguien que
    * mida los milisegundos de cada respuesta puede deducir la key caracter por
    * caracter. Para un proyecto personal es exagerado, pero cuesta una linea.
    *
    * <p>Lo que si se sigue filtrando es el largo: {@code isEqual} vuelve de
    * inmediato cuando los dos arrays miden distinto.
    */
   private boolean esValida(String recibida) {
      if (recibida == null) {
         return false;
      }
      return MessageDigest.isEqual(
            recibida.getBytes(StandardCharsets.UTF_8),
            apiKeyEsperada.getBytes(StandardCharsets.UTF_8));
   }

   /**
    * El cuerpo va en JSON y en UTF-8 porque lo unico que el atajo puede mostrar en
    * pantalla es lo que devuelva esta API. Nunca se dice cual era la key esperada ni
    * si el header venia o no: ese detalle solo le sirve a quien esta probando la
    * puerta.
    */
   private void responderNoAutorizado(HttpServletRequest peticion, HttpServletResponse respuesta)
         throws IOException {
      log.warn(
            "peticion_sin_api_key metodo={} uri={}",
            peticion.getMethod(),
            peticion.getRequestURI());

      respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
      respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
      respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
      respuesta.getWriter().write(CUERPO_401);
   }
}
