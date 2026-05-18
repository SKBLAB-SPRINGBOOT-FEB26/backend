package ru.rxyvea.backend.security.vault;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "vault")
public class VaultProperties {
    private String uri;
    private String token;
    private String kvPath;
}
