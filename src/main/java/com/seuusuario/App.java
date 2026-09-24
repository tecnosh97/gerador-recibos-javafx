package com.seuusuario;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class App extends Application {

    // Defina a versão atual do app instalada
    private final String VERSAO_ATUAL = "1.0.0"; 
    private File arquivoLogo = null;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Gerador de Recibos v" + VERSAO_ATUAL);

        if (!LicenseManager.ensureActivated(primaryStage)) {
            Platform.exit();
            return;
        }

        // Dispara a verificação de atualização no GitHub em segundo plano
        verificarAtualizacaoNoGitHub();

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setVgap(8);
        grid.setHgap(10);
        grid.setAlignment(Pos.CENTER);

        // Componentes
        ComboBox<String> cbTipoPessoa = new ComboBox<>();
        cbTipoPessoa.getItems().addAll("Cliente (CPF)", "Empresa (CNPJ)");
        cbTipoPessoa.setValue("Cliente (CPF)");
        cbTipoPessoa.setMaxWidth(Double.MAX_VALUE);

        TextField txtNome = new TextField();
        txtNome.setPromptText("Nome completo ou Razão Social");

        TextField txtDocumento = new TextField();
        txtDocumento.setPromptText("Apenas números");

        // Limite de caracteres para CPF (11) e CNPJ (14)
        txtDocumento.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtDocumento.setText(newValue.replaceAll("[^\\d]", ""));
                return;
            }
            int limite = cbTipoPessoa.getValue().equals("Cliente (CPF)") ? 11 : 14;
            if (txtDocumento.getText().length() > limite) {
                txtDocumento.setText(oldValue);
            }
        });

        cbTipoPessoa.setOnAction(e -> {
            txtDocumento.clear();
            txtDocumento.setPromptText(cbTipoPessoa.getValue().equals("Cliente (CPF)") ? "Máximo 11 números" : "Máximo 14 números");
        });

        TextField txtValor = new TextField();
        txtValor.setPromptText("Ex: 200,00");

        TextField txtValorExtenso = new TextField();
        txtValorExtenso.setPromptText("Ex: duzentos reais");

        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Ex: PAGAMENTO DE DIÁRIAS");

        ComboBox<String> cbFormaPagamento = new ComboBox<>();
        cbFormaPagamento.getItems().addAll("Dinheiro", "PIX");
        cbFormaPagamento.setValue("Dinheiro");
        cbFormaPagamento.setMaxWidth(Double.MAX_VALUE);

        TextField txtLocalData = new TextField();
        txtLocalData.setText("SÃO PAULO - SP, 08/08/2026");

        TextField txtEmitente = new TextField();
        txtEmitente.setPromptText("Seu Nome ou Empresa");

        TextField txtDocEmitente = new TextField();
        txtDocEmitente.setPromptText("Seu CPF ou CNPJ");

        // Botão para selecionar a Logo
        Button btnLogo = new Button("Selecionar Logo (Opcional)");
        Label lblLogoStatus = new Label("Nenhuma logo selecionada");
        lblLogoStatus.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");

        btnLogo.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Escolher Logo");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
            );
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                arquivoLogo = selectedFile;
                lblLogoStatus.setText(selectedFile.getName());
            }
        });

        // Adicionando ao Grid
        int row = 0;
        grid.add(new Label("Tipo de Pagador:"), 0, row); grid.add(cbTipoPessoa, 1, row++);
        grid.add(new Label("Nome / Razão:"), 0, row); grid.add(txtNome, 1, row++);
        grid.add(new Label("CPF / CNPJ:"), 0, row); grid.add(txtDocumento, 1, row++);
        grid.add(new Label("Valor (R$):"), 0, row); grid.add(txtValor, 1, row++);
        grid.add(new Label("Valor Extenso:"), 0, row); grid.add(txtValorExtenso, 1, row++);
        grid.add(new Label("Referente a:"), 0, row); grid.add(txtMotivo, 1, row++);
        grid.add(new Label("Pagamento:"), 0, row); grid.add(cbFormaPagamento, 1, row++);
        grid.add(new Label("Local e Data:"), 0, row); grid.add(txtLocalData, 1, row++);
        grid.add(new Label("Seu Nome:"), 0, row); grid.add(txtEmitente, 1, row++);
        grid.add(new Label("Seu CPF/CNPJ:"), 0, row); grid.add(txtDocEmitente, 1, row++);
        grid.add(new Label("Logo do Recibo:"), 0, row); grid.add(btnLogo, 1, row++);
        grid.add(new Label(""), 0, row); grid.add(lblLogoStatus, 1, row++);

        Button btnGerar = new Button("Gerar Recibo em PDF");
        btnGerar.setMaxWidth(Double.MAX_VALUE);
        btnGerar.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold;");
        grid.add(btnGerar, 0, row, 2, 1);

        // Ação do Botão Gerar
        btnGerar.setOnAction(e -> {
            String tipoPessoa = cbTipoPessoa.getValue();
            String nome = txtNome.getText();
            String documento = txtDocumento.getText();
            String valor = txtValor.getText();
            String valorExtenso = txtValorExtenso.getText();
            String motivo = txtMotivo.getText();
            String formaPagamento = cbFormaPagamento.getValue();
            String localData = txtLocalData.getText();
            String emitente = txtEmitente.getText();
            String docEmitente = txtDocEmitente.getText();

            if (nome.isEmpty() || valor.isEmpty() || documento.isEmpty()) {
                showAlert("Erro", "Preencha Nome, Documento e Valor!", Alert.AlertType.ERROR);
                return;
            }

            // Validação matemática de CPF implementada aqui
            if (tipoPessoa.equals("Cliente (CPF)")) {
                if (!validarCPF(documento)) {
                    showAlert("CPF Inválido", "O número de CPF informado não é matematicamente válido. Verifique os dígitos.", Alert.AlertType.ERROR);
                    return;
                }
            }

            gerarPdfProfissional(nome, documento, valor, valorExtenso, motivo, formaPagamento, localData, emitente, docEmitente, arquivoLogo);
        });

        Scene scene = new Scene(grid, 480, 560);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Algoritmo matemático para validar se o CPF é real
    private boolean validarCPF(String cpf) {
        cpf = cpf.replaceAll("\\D", "");

        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            int soma = 0;
            int peso = 10;
            for (int i = 0; i < 9; i++) {
                soma += (cpf.charAt(i) - '0') * peso--;
            }
            int resto = 11 - (soma % 11);
            int digito1 = (resto == 10 || resto == 11) ? 0 : resto;

            if (digito1 != (cpf.charAt(9) - '0')) {
                return false;
            }

            soma = 0;
            peso = 11;
            for (int i = 0; i < 10; i++) {
                soma += (cpf.charAt(i) - '0') * peso--;
            }
            resto = 11 - (soma % 11);
            int digito2 = (resto == 10 || resto == 11) ? 0 : resto;

            return digito2 == (cpf.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    private void verificarAtualizacaoNoGitHub() {
        new Thread(() -> {
            try {
                String urlApi = "https://api.github.com/repos/tecnosh97/gerador-recibos-javafx/releases/latest";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlApi))
                        .header("Accept", "application/vnd.github.v3+json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String json = response.body();
                    String tagVersion = extrairValorJson(json, "tag_name");

                    if (tagVersion != null && !tagVersion.equals("v" + VERSAO_ATUAL)) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Nova atualização disponível!");
                            alert.setHeaderText("Existe uma nova versão (" + tagVersion + ")!");
                            alert.setContentText("Baixe a nova versão no GitHub para aproveitar melhorias.");
                            alert.showAndWait();
                        });
                    }
                }
            } catch (Exception e) {
                // Silencioso se estiver offline
            }
        }).start();
    }

    private String extrairValorJson(String json, String chave) {
        try {
            String busca = "\"" + chave + "\":\"";
            int inicio = json.indexOf(busca);
            if (inicio != -1) {
                inicio += busca.length();
                int fim = json.indexOf("\"", inicio);
                return json.substring(inicio, fim);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void gerarPdfProfissional(String nome, String documento, String valor, String valorExtenso, 
                                      String motivo, String formaPagamento, String localData, 
                                      String emitente, String docEmitente, File logoFile) {
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "recibo_" + timestamp + ".pdf";
        Path outputDirectory = Path.of(System.getProperty("user.home"), "Documents", "GeradorRecibos");
        Path outputFile = outputDirectory.resolve(fileName);

        try (PDDocument document = new PDDocument()) {
            Files.createDirectories(outputDirectory);
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float margin = 40;
                float width = 612 - 2 * margin;
                float height = 440; 
                float startY = 750 - height;

                cs.setLineWidth(1.5f);
                cs.addRect(margin, startY, width, height);
                cs.stroke();

                if (logoFile != null && logoFile.exists()) {
                    PDImageXObject pdImage = PDImageXObject.createFromFile(logoFile.getAbsolutePath(), document);
                    cs.drawImage(pdImage, margin + 15, startY + height - 60, 50, 50);
                }

                float boxWidth = 155;
                float boxHeight = 35;
                float boxX = 612 - margin - boxWidth - 15;
                float boxY = startY + height - 50;

                cs.setLineWidth(1f);
                cs.addRect(boxX, boxY, boxWidth, boxHeight);
                cs.stroke();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.newLineAtOffset(boxX + 12, boxY + 12);
                cs.showText("VALOR: R$ " + valor);
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                cs.newLineAtOffset(margin + 175, startY + height - 32);
                cs.showText("RECIBO");
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.newLineAtOffset(margin + 200, startY + height - 50);
                cs.showText("Nº " + timestamp.substring(11, 19).replace("-", ""));
                cs.endText();

                cs.setLineWidth(1f);
                cs.moveTo(margin + 15, startY + height - 70);
                cs.lineTo(612 - margin - 15, startY + height - 70);
                cs.stroke();

                float textX = margin + 25;
                float textY = startY + height - 105;

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.newLineAtOffset(textX, textY);
                cs.showText("Recebi(emos) de " + nome.toUpperCase() + ", inscrito(a) no CPF/CNPJ");
                cs.newLineAtOffset(0, -18);
                cs.showText("sob o nº " + documento + ", a importância de R$ " + valor + " (" + valorExtenso + "),");
                cs.newLineAtOffset(0, -18);
                cs.showText("referente a " + motivo.toUpperCase() + ".");
                cs.endText();

                float p2Y = textY - 65;
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.newLineAtOffset(textX, p2Y);
                cs.showText("Para maior clareza, firmo(amos) o presente documento para que produza os");
                cs.newLineAtOffset(0, -18);
                cs.showText("seus efeitos legais, dando plena, rasa e geral quitação pelo valor acima recebido.");
                cs.endText();

                float fpY = p2Y - 45;
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.newLineAtOffset(textX, fpY);
                cs.showText("Forma de Pagamento: ");
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.showText(formaPagamento);
                cs.endText();

                float dateY = fpY - 35;
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.newLineAtOffset(340, dateY);
                cs.showText(localData);
                cs.endText();

                float sigLineY = dateY - 45;
                cs.setLineWidth(0.75f);
                cs.moveTo(180, sigLineY);
                cs.lineTo(430, sigLineY);
                cs.stroke();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.newLineAtOffset(textX + 70, sigLineY - 15);
                cs.showText(emitente.toUpperCase());
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(textX + 115, sigLineY - 30);
                cs.showText("CPF/CNPJ: " + docEmitente);
                cs.endText();
            }

            document.save(outputFile.toFile());
            showAlert("Sucesso", "PDF gerado em:\n" + outputFile.toAbsolutePath(), Alert.AlertType.INFORMATION);

        } catch (IOException | RuntimeException e) {
            showAlert("Erro", "Erro ao gerar PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
