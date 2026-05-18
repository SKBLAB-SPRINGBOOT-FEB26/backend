package ru.rxyvea.backend.security.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.rxyvea.backend.security.props.helpers.EcKeyReader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads (and on first run, seeds) ECDSA P-256 JWT keys in HashiCorp Vault KV v2.
 *
 * Uses a JDK-native HttpClient instead of spring-vault-core because the latter
 * has not yet released a Spring Boot 4 / Spring 7 compatible artifact.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class VaultJwtKeys {
    private static final String PRIVATE_KEY = "private-key-pem";
    private static final String PUBLIC_KEY = "public-key-pem";
    private static final String CURVE = "secp256r1";

    private static final ObjectMapper JSON = new ObjectMapper();

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final VaultProperties props;

    @Bean
    public KeyMaterial jwtKeyMaterial() throws IOException, InterruptedException, GeneralSecurityException {
        final var http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        final var dataUrl = buildDataUrl(props.getUri(), props.getKvPath());

        final var existing = readKv(http, dataUrl);
        if (existing != null) {
            final var priv = (String) existing.get(PRIVATE_KEY);
            final var pub = (String) existing.get(PUBLIC_KEY);
            if (priv != null && pub != null) {
                log.info("Loaded JWT EC keys from Vault path '{}'", props.getKvPath());
                return new KeyMaterial(
                        EcKeyReader.privateKeyFromPem(priv),
                        EcKeyReader.publicKeyFromPem(pub)
                );
            }
        }

        log.warn("Vault path '{}' is empty — generating a fresh ECDSA P-256 keypair and seeding it.",
                props.getKvPath());

        final var generator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(new ECGenParameterSpec(CURVE));
        final KeyPair generated = generator.generateKeyPair();

        final var privPem = EcKeyReader.toPem(generated.getPrivate());
        final var pubPem = EcKeyReader.toPem(generated.getPublic());

        final Map<String, Object> data = new LinkedHashMap<>();
        data.put(PRIVATE_KEY, privPem);
        data.put(PUBLIC_KEY, pubPem);
        writeKv(http, dataUrl, data);

        log.info("Seeded JWT EC keys at Vault path '{}'", props.getKvPath());

        return new KeyMaterial(
                EcKeyReader.privateKeyFromPem(privPem),
                EcKeyReader.publicKeyFromPem(pubPem)
        );
    }

    @Bean
    public ECPrivateKey jwtPrivateKey(KeyMaterial material) {
        return material.privateKey();
    }

    @Bean
    public ECPublicKey jwtPublicKey(KeyMaterial material) {
        return material.publicKey();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readKv(HttpClient http, URI url) throws IOException, InterruptedException {
        final var req = HttpRequest.newBuilder(url)
                .GET()
                .header("X-Vault-Token", props.getToken())
                .timeout(Duration.ofSeconds(5))
                .build();
        final var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 404) {
            return null;
        }
        if (resp.statusCode() != 200) {
            throw new IOException("Vault GET %s -> %d: %s".formatted(url, resp.statusCode(), resp.body()));
        }
        final var root = JSON.readValue(resp.body(), Map.class);
        final var outer = (Map<String, Object>) root.get("data");
        if (outer == null) return null;
        return (Map<String, Object>) outer.get("data");
    }

    private void writeKv(HttpClient http, URI url, Map<String, Object> data) throws IOException, InterruptedException {
        final var payload = JSON.writeValueAsString(Map.of("data", data));
        final var req = HttpRequest.newBuilder(url)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .header("X-Vault-Token", props.getToken())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .build();
        final var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("Vault POST %s -> %d: %s".formatted(url, resp.statusCode(), resp.body()));
        }
    }

    private static URI buildDataUrl(String vaultUri, String fullPath) {
        final var i = fullPath.indexOf('/');
        if (i < 0) {
            throw new IllegalStateException("vault.kv-path must be '<mount>/<path>', got: " + fullPath);
        }
        final var mount = fullPath.substring(0, i);
        final var inner = fullPath.substring(i + 1);
        return URI.create("%s/v1/%s/data/%s".formatted(stripTrailingSlash(vaultUri), mount, inner));
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    public record KeyMaterial(ECPrivateKey privateKey, ECPublicKey publicKey) {}
}
