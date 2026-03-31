package ru.rxyvea.backend.security.props.helpers;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class EcKeyReader {
    public static ECPrivateKey privateKey(Resource privateKeyRes) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(privateKeyRes.getInputStream())) {
            final var pemParser = new PEMParser(reader);
            final var converter = new JcaPEMKeyConverter();

            final var keyPair = (PEMKeyPair) pemParser.readObject();
            return (ECPrivateKey) converter.getPrivateKey(keyPair.getPrivateKeyInfo());
        }
    }

    public static ECPublicKey publicKey(Resource publicKeyRes) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(publicKeyRes.getInputStream())) {
            final var pemParser = new PEMParser(reader);
            final var converter = new JcaPEMKeyConverter();

            final var publicKeyInfo = SubjectPublicKeyInfo.getInstance(pemParser.readObject());
            return (ECPublicKey) converter.getPublicKey(publicKeyInfo);
        }
    }
}
