package com.tucan.api.controller;

import com.tucan.api.model.Categoria;
import com.tucan.api.model.MovimientoRequest;
import com.tucan.api.model.MovimientoResponse;
import com.tucan.api.model.TipoMovimiento;
import com.tucan.api.service.MovimientoService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La puerta de entrada HTTP de la API. Recibe, dispara la validacion y delega.
 *
 * <p>No tiene ni un {@code if} sobre los datos a proposito: la coherencia entre
 * tipo y categoria vive en {@link MovimientoRequest}, el reparto de categorias en
 * {@link Categoria} y la escritura en {@link MovimientoService}.
 *
 * <p>El servicio entra por constructor. Desde Spring 4.3 una clase con un solo
 * constructor no necesita {@code @Autowired}.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

   private final MovimientoService servicio;

   public ApiController(MovimientoService servicio) {
      this.servicio = servicio;
   }

   /** Sirve para saber desde afuera si la API esta despierta. En FIN-19 queda sin API key. */
   @GetMapping(value = "/ping", produces = MediaType.TEXT_PLAIN_VALUE)
   public String ping() {
      return "pong";
   }

   /**
    * Las etiquetas para los menus del atajo. Sin parametro devuelve las dos ramas
    * juntas, en una sola peticion; con {@code ?tipo=Gasto} o {@code ?tipo=GASTO},
    * solo esa.
    */
   @GetMapping("/categorias")
   public Map<TipoMovimiento, List<String>> categorias(
         @RequestParam(required = false) TipoMovimiento tipo) {
      return Categoria.etiquetasPorTipo(tipo);
   }

   /**
    * El endpoint principal: guarda un movimiento y devuelve 201.
    *
    * <p>El {@code @Valid} no es decorativo. Sin el, las reglas de FIN-13 y FIN-14 se
    * ignoran en silencio y la hoja se llena de basura sin que nada falle.
    *
    * <p>La {@link IOException} del servicio se declara y sube: que responder cuando
    * Google falla se decide en un solo lugar, en FIN-20.
    */
   @PostMapping("/movimientos")
   @ResponseStatus(HttpStatus.CREATED)
   public MovimientoResponse registrar(@Valid @RequestBody MovimientoRequest movimiento)
         throws IOException {
      servicio.agregar(movimiento);
      return MovimientoResponse.de(movimiento);
   }
}
