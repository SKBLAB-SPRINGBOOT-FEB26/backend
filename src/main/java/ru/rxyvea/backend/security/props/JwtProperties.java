package ru.rxyvea.backend.security.props;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import ru.rxyvea.backend.security.props.helpers.EcKeyReader;

import java.io.IOException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private Long expiry;
    private Long refreshExpiry;

    private Resource privateKeyRes;
    private Resource publicKeyRes;

    public ECPrivateKey privateKey() throws IOException {
        return EcKeyReader.privateKey(privateKeyRes);
    }

    public ECPublicKey publicKey() throws IOException {
        return EcKeyReader.publicKey(publicKeyRes);
    }
}
