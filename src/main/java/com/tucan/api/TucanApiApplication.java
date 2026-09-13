package com.tucan.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TucanApiApplication {

   public static void main(String[] args) {
      SpringApplication.run(TucanApiApplication.class, args);
   }

}
