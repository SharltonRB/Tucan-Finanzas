package com.tucan.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * El cuerpo de cualquier respuesta de error de la API.
 *
 * <p>Tener un solo formato para todos los errores es lo que permite que el atajo
 * del iPhone lea siempre {@code error} y muestre eso en pantalla, sin saber que
 * salio mal ni tener que mirar el status.
 *
 * <p>Los campos que no aplican no viajan: con {@code NON_NULL}, un 500 manda
 * solamente {@code error} en vez de arrastrar dos nulos que el atajo tendria que
 * ignorar.
 *
 * <ul>
 *   <li>{@code error}: la frase que se muestra. Siempre viene.
 *   <li>{@code detalle}: la causa concreta, cuando decirla ayuda a corregir.
 *   <li>{@code campos}: que campo fallo y por que, cuando fue la validacion.
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaDeError(String error, String detalle, Map<String, String> campos) {

   /** Para el 500: lo unico que sale es la frase neutra. */
   public static RespuestaDeError de(String error) {
      return new RespuestaDeError(error, null, null);
   }

   /** Para los errores donde la causa concreta le sirve a quien mando la peticion. */
   public static RespuestaDeError con(String error, String detalle) {
      return new RespuestaDeError(error, detalle, null);
   }

   /**
    * Para las fallas de validacion. Se copia el mapa: quien lo arma no deberia
    * poder cambiarlo despues de que la respuesta ya existe.
    */
   public static RespuestaDeError deCampos(String error, Map<String, String> campos) {
      return new RespuestaDeError(error, null, Map.copyOf(campos));
   }
}
