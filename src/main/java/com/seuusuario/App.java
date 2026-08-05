package com.seuusuario;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Gerador de Recibos Profissional - JavaFX");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setVgap(10);
        grid.setHgap(10);

        TextField txtNome = new TextField();
        txtNome.setPromptText("Ex: JOÃO FERREIRA");

        TextField txtDocumento = new TextField();
        txtDocumento.setPromptText("Ex: 123.456.789-00");

        TextField txtValor = new TextField();
        txtValor.setPromptText("Ex: 200,00");

        TextField txtValorExtenso = new TextField();
        txtValorExtenso.setPromptText("Ex: duzentos reais");

        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Ex: PAGAMENTO DE DIÁRIAS");

        TextField txtFormaPagamento = new TextField();
        txtFormaPagamento.setText("Dinheiro / PIX");

        TextField txtLocalData = new TextField();
        txtLocalData.setText("SÃO PAULO - SP, 05/08/2026");

        TextField txtEmitente = new TextField();
        txtEmitente.setPromptText("Seu Nome ou Empresa");

        TextField txtDocEmitente = new TextField();
        txtDocEmitente.setPromptText("Seu CPF ou CNPJ");

        grid.add(new Label("Nome do Pagador:"), 0, 0);
        grid.add(txtNome, 1, 0);

        grid.add(new Label("CPF/CNPJ do Pagador:"), 0, 1);
        grid.add(txtDocumento, 1, 1);

        grid.add(new Label("Valor (R$):"), 0, 2);
        grid.add(txtValor, 1, 2);

        grid.add(new Label("Valor por Extenso:"), 0, 3);
        grid.add(txtValorExtenso, 1, 3);

        grid.add(new Label("Referente a:"), 0, 4);
        grid.add(txtMotivo, 1, 4);

        grid.add(new Label("Forma de Pagamento:"), 0, 5);
        grid.add(txtFormaPagamento, 1, 5);

        grid.add(new Label("Local e Data:"), 0, 6);
        grid.add(txtLocalData, 1, 6);

        grid.add(new Label("Seu Nome/Empresa:"), 0, 7);
        grid.add(txtEmitente, 1, 7);

        grid.add(new Label("Seu CPF/CNPJ:"), 0, 8);
        grid.add(txtDocEmitente, 1, 8);

        Button btnGerar = new Button("Gerar Recibo em PDF");
        grid.add(btnGerar, 1, 9);

        btnGerar.setOnAction(e -> {
            String nome = txtNome.getText();
            String documento = txtDocumento.getText();
            String valor = txtValor.getText();
            String valorExtenso = txtValorExtenso.getText();
            String motivo = txtMotivo.getText();
            String formaPagamento = txtFormaPagamento.getText();
            String localData = txtLocalData.getText();
            String emitente = txtEmitente.getText();
            String docEmitente = txtDocEmitente.getText();

            if (nome.isEmpty() || valor.isEmpty()) {
                showAlert("Erro", "Preencha pelo menos o Nome e o Valor!", Alert.AlertType.ERROR);
                return;
            }

            gerarPdfProfissional(nome, documento, valor, valorExtenso, motivo, formaPagamento, localData, emitente, docEmitente);
        });

        Scene scene = new Scene(grid, 500, 550);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void gerarPdfProfissional(String nome, String documento, String valor, String valorExtenso, 
                                      String motivo, String formaPagamento, String localData, 
                                      String emitente, String docEmitente) {
        String fileName = "recibo.pdf";

        try (PDDocument document = new PDDocument()) {
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
                cs.showText("Nº 001");
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

            document.save(fileName);
            showAlert("Sucesso", "PDF gerado com sucesso na pasta do projeto!", Alert.AlertType.INFORMATION);

        } catch (IOException e) {
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
