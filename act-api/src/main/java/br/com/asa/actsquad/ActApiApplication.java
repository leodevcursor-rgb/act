package br.com.asa.actsquad;

import br.com.asa.actsquad.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ActApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(ActApiApplication.class, args);
  }
}
