package br.com.fiap.siase.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "br.com.fiap.siase")
@EnableJpaRepositories(basePackages = "br.com.fiap.siase.infrastructure.persistence")
@EntityScan(basePackages = "br.com.fiap.siase.domain.model")
public class SiaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiaseApplication.class, args);
    }
}
