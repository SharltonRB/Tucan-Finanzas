package com.tucan.api.model;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Traduce el texto que llega por HTTP a una constante de enum.
 *
 * <p>En la hoja las etiquetas van sin tildes y el atajo manda exactamente eso,
 * pero desde Bruno o curl alguien va a escribir "Alimentacion" con tilde, en
 * minusculas o con espacios de sobra. Todo eso tiene que resolver al mismo valor.
 *
 * <p>La receta: separar cada letra de su tilde con NFD, borrar las marcas
 * diacriticas, recortar, pasar a mayusculas y convertir los separadores en guion
 * bajo. Con ese ultimo paso la etiqueta normalizada queda igual al nombre de la
 * constante ("Ahorro/Inversion" a AHORRO_INVERSION, "SINPE Movil" a SINPE_MOVIL),
 * asi que alcanza con comparar contra {@code name()}.
 *
 * <p>Por eso no hay ningun mapa ni {@code switch} escrito a mano: una categoria
 * nueva funciona sola, sin tocar este archivo.
 *
 * <p>Vive aparte y no duplicada en cada enum porque son tres los que la necesitan.
 */
final class Etiquetas {

   private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}");
   private static final Pattern SEPARADORES = Pattern.compile("[^A-Z0-9]+");

   private Etiquetas() {
   }

   /**
    * Devuelve la constante que corresponde al texto recibido.
    *
    * <p>Con un texto nulo o en blanco devuelve {@code null} en vez de fallar: el
    * parser no es quien decide si un valor ausente es aceptable. De eso se encarga
    * despues la validacion, con un mensaje mucho mas claro que una excepcion de
    * deserializacion.
    *
    * @throws IllegalArgumentException si el texto no corresponde a ninguna
    *     constante. El mensaje incluye lo que llego, que es lo unico que sirve
    *     cuando el error aparece desde el iPhone.
    */
   static <E extends Enum<E>> E resolver(E[] valores, String recibido, String queNoSeReconoce) {
      if (recibido == null || recibido.isBlank()) {
         return null;
      }
      String clave = normalizar(recibido);
      return Arrays.stream(valores)
            .filter(valor -> valor.name().equals(clave))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                  queNoSeReconoce + ": \"" + recibido + "\""));
   }

   private static String normalizar(String texto) {
      String descompuesto = Normalizer.normalize(texto, Normalizer.Form.NFD);
      String sinTildes = MARCAS_DIACRITICAS.matcher(descompuesto).replaceAll("");
      // El Locale explicito no es decorativo: sin el, en turco la "i" mayuscula
      // no es la que uno espera, y es un bug clasico.
      String enMayusculas = sinTildes.trim().toUpperCase(Locale.ROOT);
      return SEPARADORES.matcher(enMayusculas).replaceAll("_");
   }
}
