package com.seuusuario;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

final class LicenseManager {
    private static final String PUBLIC_KEY = "MCowBQYDK2VwAyEA5iSbU_4cOi4VXfwE54dQCSqp58awRqtspgz76e1he4M";
    private static final String LICENSE_PREFIX = "GR1";
    private static final Path LICENSE_FILE = getLicenseFile();

    private LicenseManager() {
    }

    static boolean ensureActivated(Window owner) {
        try {
            if (Files.exists(LICENSE_FILE) && isValid(Files.readString(LICENSE_FILE).trim())) {
                return true;
            }
        } catch (Exception ignored) {
        }

        while (true) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.initOwner(owner);
            dialog.setTitle("Ativação do Gerador de Recibos");
            dialog.setHeaderText("Informe a chave recebida após a compra");
            dialog.setContentText("Chave de ativação:");

            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return false;
            }

            String key = result.get().trim();
            if (isValid(key)) {
                try {
                    Files.createDirectories(LICENSE_FILE.getParent());
                    Files.writeString(LICENSE_FILE, key, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    return true;
                } catch (Exception exception) {
                    showError(owner, "Não foi possível salvar a ativação neste computador.");
                    return false;
                }
            }

            showError(owner, "A chave é inválida, foi digitada incorretamente ou está vencida.");
        }
    }

    private static boolean isValid(String license) {
        try {
            String[] parts = license.split("\\.", -1);
            if (parts.length != 2) {
                return false;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            String[] fields = payload.split("\\|", -1);

            if (fields.length != 3 || !LICENSE_PREFIX.equals(fields[0]) || fields[1].isBlank()) {
                return false;
            }

            if (!"PERMANENTE".equals(fields[2]) && LocalDate.parse(fields[2]).isBefore(LocalDate.now())) {
                return false;
            }

            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(Base64.getUrlDecoder().decode(PUBLIC_KEY)));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(payloadBytes);
            return verifier.verify(signatureBytes);
        } catch (Exception exception) {
            return false;
        }
    }

    private static Path getLicenseFile() {
        String appData = System.getenv("APPDATA");
        Path base = appData == null || appData.isBlank()
                ? Path.of(System.getProperty("user.home"))
                : Path.of(appData);
        return base.resolve("GeradorRecibos").resolve("license.key");
    }

    private static void showError(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("Ativação inválida");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}