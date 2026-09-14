package com.tucan.api;

import com.google.api.services.sheets.v4.Sheets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class TucanApiApplicationTests {

   /**
    * El cliente de Sheets se sustituye por un mock a proposito. El
    * application.yml de test trae credenciales ficticias, y construir el bean de
    * verdad obligaria a meter una llave privada real en el repositorio solo para
    * que arranque el contexto.
    */
   @MockBean
   private Sheets sheets;

   @Test
   void contextLoads() {
   }

}
