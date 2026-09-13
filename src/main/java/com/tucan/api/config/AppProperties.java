package com.tucan.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de la aplicacion, leida del entorno via application.yml.
 *
 * <p>Se valida al construirse, durante el arranque del contexto: si falta una
 * variable obligatoria la aplicacion no arranca, en vez de fallar en la primera
 * peticion desde el iPhone.
 *
 * <p>La comprobacion contra {@code "${"} no es un capricho: el binder de
 * {@code @ConfigurationProperties} ignora los placeholders que no puede resolver
 * y deja el texto literal {@code "${API_KEY}"} como valor, asi que una
 * validacion de "no vacio" pasaria sin notar que falta la variable.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String apiKey, String zonaHoraria, Google google) {

   private static final String PLACEHOLDER_SIN_RESOLVER = "${";

   public AppProperties {
      exigir(apiKey, "API_KEY");
      exigir(zonaHoraria, "app.zona-horaria");
   }

   /** Todo lo necesario para hablar con la hoja de calculo. */
   public record Google(String credentialsBase64, String spreadsheetId, String hoja) {

      public Google {
         exigir(credentialsBase64, "GOOGLE_CREDENTIALS_BASE64");
         exigir(spreadsheetId, "SPREADSHEET_ID");
         exigir(hoja, "app.google.hoja");
      }
   }

   private static void exigir(String valor, String nombre) {
      if (valor == null || valor.isBlank() || valor.startsWith(PLACEHOLDER_SIN_RESOLVER)) {
         throw new IllegalStateException(
               "Falta la configuracion obligatoria '" + nombre
                     + "'. Definila como variable de entorno antes de arrancar la aplicacion.");
      }
   }
}
