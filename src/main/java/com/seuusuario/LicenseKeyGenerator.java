package com.seuusuario;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;

public final class LicenseKeyGenerator {
    private LicenseKeyGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Uso: java ...LicenseKeyGenerator <chave-privada.key> <cliente> <dias|PERMANENTE>");
            System.exit(1);
        }

        String customer = args[1].trim().replace("|", " ");
        String expiration = "PERMANENTE".equalsIgnoreCase(args[2])
                ? "PERMANENTE"
                : LocalDate.now().plusDays(Long.parseLong(args[2])).toString();
        String payload = "GR1|" + customer + "|" + expiration;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        byte[] privateKeyBytes = Base64.getUrlDecoder().decode(Files.readString(Path.of(args[0])).trim());
        PrivateKey privateKey = KeyFactory.getInstance("Ed25519")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(privateKey);
        signer.update(payloadBytes);

        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);
        String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        System.out.println(encodedPayload + "." + encodedSignature);
    }
}