package com.tucan.api.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma el cliente autenticado de Google Sheets.
 *
 * <p>Se construye una sola vez, al arrancar. Si se hiciera por peticion, cada
 * gasto anotado desde el iPhone implicaria negociar un token nuevo con Google.
 *
 * <p>Las credenciales salen de una variable de entorno y nunca de un archivo del
 * proyecto. Guardar la llave privada en el repositorio dejaria el ticket
 * "funcionando" y volveria imposible el despliegue de FIN-26 sin meter la llave
 * dentro de la imagen de Docker.
 */
@Configuration
public class GoogleSheetsConfig {

   /** Solo aparece en los logs de Google; conviene que sea identificable. */
   private static final String NOMBRE_APLICACION = "tucan-api";

   private final AppProperties propiedades;

   public GoogleSheetsConfig(AppProperties propiedades) {
      this.propiedades = propiedades;
   }

   @Bean
   public Sheets sheets() {
      GoogleCredentials credenciales = credencialesDesde(propiedades.google().credentialsBase64());
      return new Sheets.Builder(
            transporte(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credenciales))
            .setApplicationName(NOMBRE_APLICACION)
            .build();
   }

   /**
    * Decodifica la variable de entorno y construye las credenciales con el
    * permiso de escritura sobre hojas.
    *
    * <p>El {@code createScoped} es facil de olvidar y el sintoma no es obvio: sin
    * el, las credenciales son perfectamente validas pero no tienen permiso para
    * nada, y el error que devuelve Google no menciona los scopes.
    *
    * <p>Los dos fallos posibles se traducen a un mensaje que nombra la variable.
    * Cuando esto reviente al arrancar en Cloud Run, ese mensaje es lo unico que
    * vas a tener para saber por donde empezar.
    */
   static GoogleCredentials credencialesDesde(String credencialesEnBase64) {
      byte[] credenciales = decodificar(credencialesEnBase64);
      try {
         return GoogleCredentials.fromStream(new ByteArrayInputStream(credenciales))
               .createScoped(List.of(SheetsScopes.SPREADSHEETS));
      } catch (IOException e) {
         throw new IllegalStateException(
               "GOOGLE_CREDENTIALS_BASE64 no contiene unas credenciales de cuenta de servicio"
                     + " validas: " + e.getMessage(), e);
      }
   }

   private static byte[] decodificar(String credencialesEnBase64) {
      try {
         return Base64.getDecoder().decode(credencialesEnBase64);
      } catch (IllegalArgumentException e) {
         throw new IllegalStateException(
               "GOOGLE_CREDENTIALS_BASE64 no es base64 valido. Tiene que ser una sola linea,"
                     + " sin saltos. Regeneralo con:"
                     + " base64 -i tucan-credenciales.json | tr -d '\\n'", e);
      }
   }

   private static HttpTransport transporte() {
      try {
         return GoogleNetHttpTransport.newTrustedTransport();
      } catch (GeneralSecurityException | IOException e) {
         throw new IllegalStateException("No se pudo construir el transporte HTTP hacia Google", e);
      }
   }
}
