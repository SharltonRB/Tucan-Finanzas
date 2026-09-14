package com.tucan.api.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.tucan.api.model.RespuestaDeError;
import java.io.IOException;
import java.time.DateTimeException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce cada excepcion a una respuesta que se pueda leer en la pantalla de un
 * iPhone.
 *
 * <p>Esta clase es la interfaz de usuario de los casos de error: el atajo no tiene
 * pantalla propia, solo muestra lo que devuelva la API. Por eso los mensajes van en
 * espanol, dicen que hacer, y el formato es siempre el mismo
 * ({@link RespuestaDeError}).
 *
 * <p>Spring elige siempre el handler mas especifico que encaje, asi que el de
 * {@link Exception} de abajo no se come a los demas por estar declarado en la misma
 * clase.
 *
 * <p>La linea que separa los status: 4xx es culpa de quien llamo y conviene decirle
 * exactamente que estuvo mal; 502 es culpa de Google, no nuestra; 500 es culpa
 * nuestra y ahi el detalle se calla.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

   private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

   private static final String SIN_MOTIVO = "valor invalido";

   /** Lo que se dice cuando el cuerpo ni siquiera es JSON valido. Ver {@link #ilegible}. */
   private static final String JSON_MAL_FORMADO =
         "El JSON esta mal armado. Revisa las llaves, las comillas y las comas.";

   /** Para una fecha que no existe o que viene en otro formato. Ver {@link #ilegible}. */
   private static final String FECHA_INVALIDA =
         "La fecha no existe o no viene como aaaa-mm-dd. Ejemplo: 2026-09-11.";

   /**
    * Lo unico que ve el cliente cuando la culpa es del servidor. Deliberadamente no
    * dice nada: el detalle podria nombrar rutas, clases o parte de una credencial.
    */
   private static final String ERROR_NUESTRO =
         "Algo salio mal en el servidor. El movimiento no se guardo, intenta de nuevo.";

   /**
    * Las fallas de {@code @NotNull}, {@code @DecimalMin} y {@code @AssertTrue}
    * llegan todas aca juntas.
    *
    * <p>Se devuelven todos los campos que fallaron y no solo el primero: corregir de
    * a un error por viaje, desde un telefono, es exactamente la clase de detalle que
    * hace que una herramienta se deje de usar.
    *
    * <p>El {@link LinkedHashMap} conserva el orden en que el modelo declara los
    * campos. El {@code merge} contempla un mismo campo con dos reglas rotas a la
    * vez: sin el, la llave repetida tumbaria la respuesta y el 400 terminaria siendo
    * un 500.
    */
   @ExceptionHandler(MethodArgumentNotValidException.class)
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   public RespuestaDeError validacion(MethodArgumentNotValidException excepcion) {
      Map<String, String> campos = new LinkedHashMap<>();

      for (FieldError falla : excepcion.getBindingResult().getFieldErrors()) {
         campos.merge(falla.getField(), motivoDe(falla), (primero, otro) -> primero + "; " + otro);
      }

      return RespuestaDeError.deCampos("Hay datos invalidos en el movimiento", campos);
   }

   /**
    * JSON roto, o un valor que no corresponde a ninguna etiqueta conocida.
    *
    * <p>Los dos casos llegan por la misma excepcion y no merecen la misma respuesta,
    * asi que se miran por separado segun la causa de mas abajo:
    *
    * <ul>
    *   <li>Sintaxis rota ({@link JsonParseException}): el mensaje de Jackson dice
    *       "Unexpected end-of-input ... StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION",
    *       en ingles y nombrando sus propias clases. Eso no se le manda a nadie.
    *   <li>Fecha imposible ({@link DateTimeException}): la causa de mas abajo dice
    *       "Invalid value for MonthOfYear (valid values 1 - 12): 13", en ingles y sin
    *       repetir la fecha que llego. Tampoco sirve.
    *   <li>Cualquier otra causa: es un valor puntual que no se pudo interpretar, y
    *       ahi el mensaje de abajo si es el bueno. El de {@code Etiquetas} viene en
    *       espanol y con el valor recibido entre comillas, que es justo lo que hay
    *       que corregir.
    * </ul>
    */
   @ExceptionHandler(HttpMessageNotReadableException.class)
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   public RespuestaDeError ilegible(HttpMessageNotReadableException excepcion) {
      String detalle = switch (NestedExceptionUtils.getMostSpecificCause(excepcion)) {
         case JsonParseException sintaxisRota -> JSON_MAL_FORMADO;
         case DateTimeException fechaImposible -> FECHA_INVALIDA;
         default -> causaDe(excepcion);
      };

      return RespuestaDeError.con("No se pudo leer el movimiento que mandaste", detalle);
   }

   /**
    * El {@code ?tipo=} de la URL con un valor que no existe. Es el mismo error que
    * el de arriba pero por la otra puerta, y merece la misma respuesta: sin este
    * handler caeria en el generico, y un error del cliente se veria como un 500.
    */
   @ExceptionHandler(MethodArgumentTypeMismatchException.class)
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   public RespuestaDeError parametroInvalido(MethodArgumentTypeMismatchException excepcion) {
      return RespuestaDeError.con(
            "El parametro " + excepcion.getName() + " no es valido", causaDe(excepcion));
   }

   /**
    * Google fallo. Un 502 es mas honesto que un 500: el que se cayo fue un servicio
    * del que dependemos, no esta API.
    *
    * <p>Aca el detalle si sale, porque sirve para distinguir una hoja mal
    * configurada de un permiso faltante, y porque no lo escribio un desconocido.
    */
   @ExceptionHandler(IOException.class)
   @ResponseStatus(HttpStatus.BAD_GATEWAY)
   public RespuestaDeError fallaGoogle(IOException excepcion) {
      log.error("fallo_google_sheets", excepcion);

      return RespuestaDeError.con(
            "No se pudo guardar en la hoja. Google no respondio bien.", excepcion.getMessage());
   }

   /**
    * La red de contencion: cualquier cosa que no encaje arriba.
    *
    * <p>El mensaje de la excepcion se registra y no se devuelve. Un mensaje interno
    * puede traer una ruta del servidor o parte de una credencial, y mandarselo a
    * quien esta probando la puerta es regalarle el mapa.
    *
    * <p>Las excepciones propias de Spring (404, 405, 415) implementan
    * {@link ErrorResponse} y ya vienen con su status correcto. Se respeta: sin esta
    * rama, pedir una ruta que no existe pasaria de responder 404 a responder 500,
    * que seria mentir sobre de quien fue la culpa.
    */
   @ExceptionHandler(Exception.class)
   public ResponseEntity<RespuestaDeError> inesperado(Exception excepcion) {
      if (excepcion instanceof ErrorResponse deSpring) {
         return ResponseEntity.status(deSpring.getStatusCode())
               .body(RespuestaDeError.con("La peticion no se pudo atender", causaDe(excepcion)));
      }

      log.error("error_inesperado", excepcion);

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RespuestaDeError.de(ERROR_NUESTRO));
   }

   /**
    * El mensaje que escribio el modelo. El nulo solo aparece si alguien declara una
    * anotacion sin {@code message}, pero un mapa con un nulo adentro hace fallar a
    * {@code Map.copyOf} y tumbaria la respuesta entera.
    */
   private static String motivoDe(FieldError falla) {
      return falla.getDefaultMessage() == null ? SIN_MOTIVO : falla.getDefaultMessage();
   }

   private static String causaDe(Throwable excepcion) {
      Throwable raiz = NestedExceptionUtils.getMostSpecificCause(excepcion);
      return raiz.getMessage() == null ? SIN_MOTIVO : raiz.getMessage();
   }
}
