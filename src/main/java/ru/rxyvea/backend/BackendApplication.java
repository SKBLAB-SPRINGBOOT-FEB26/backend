package ru.rxyvea.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.rxyvea.backend.security.props.JwtProperties;
import ru.rxyvea.backend.security.vault.VaultProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, VaultProperties.class})
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
