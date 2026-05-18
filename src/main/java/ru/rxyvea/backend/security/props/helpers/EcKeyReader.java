package ru.rxyvea.backend.security.props.helpers;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public final class EcKeyReader {
    private EcKeyReader() {}

    public static ECPrivateKey privateKey(Resource res) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(res.getInputStream())) {
            return parsePrivate(reader);
        }
    }

    public static ECPublicKey publicKey(Resource res) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(res.getInputStream())) {
            return parsePublic(reader);
        }
    }

    public static ECPrivateKey privateKeyFromPem(String pem) throws IOException {
        try (StringReader reader = new StringReader(pem)) {
            return parsePrivate(reader);
        }
    }

    public static ECPublicKey publicKeyFromPem(String pem) throws IOException {
        try (StringReader reader = new StringReader(pem)) {
            return parsePublic(reader);
        }
    }

    public static String toPem(PrivateKey key) throws IOException {
        try (StringWriter sw = new StringWriter(); JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(key);
            pw.flush();
            return sw.toString();
        }
    }

    public static String toPem(PublicKey key) throws IOException {
        try (StringWriter sw = new StringWriter(); JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(key);
            pw.flush();
            return sw.toString();
        }
    }

    private static ECPrivateKey parsePrivate(Reader reader) throws IOException {
        final var pemParser = new PEMParser(reader);
        final var converter = new JcaPEMKeyConverter();
        final var keyPair = (PEMKeyPair) pemParser.readObject();
        return (ECPrivateKey) converter.getPrivateKey(keyPair.getPrivateKeyInfo());
    }

    private static ECPublicKey parsePublic(Reader reader) throws IOException {
        final var pemParser = new PEMParser(reader);
        final var converter = new JcaPEMKeyConverter();
        final var publicKeyInfo = SubjectPublicKeyInfo.getInstance(pemParser.readObject());
        return (ECPublicKey) converter.getPublicKey(publicKeyInfo);
    }
}
