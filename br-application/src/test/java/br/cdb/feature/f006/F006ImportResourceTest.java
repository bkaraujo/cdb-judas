package br.cdb.feature.f006;

import br.cdb.PdfFixtures;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Porta de entrada do import ({@code POST /accounts/transactions/import/preview}): o que acontece com
 * o arquivo antes de qualquer linha ser lida — falhas de extração do PDF mapeadas para código de erro
 * — e o roteamento do documento, isto é, qual parser reivindica cada arquivo. Um documento real tem de
 * ser reivindicado por exatamente um parser (emissor + tipo de documento juntos); texto ambíguo ou
 * irreconhecível não pode ser reivindicado por nenhum, o que vira {@code UNKNOWN_ISSUER}.
 */
@QuarkusTest
public class F006ImportResourceTest extends AbstractImportTest {

    /** Fatura BTG mínima, suficiente para o roteador identificar emissor e tipo. */
    private static final String FATURA_BTG = "BTG Pactual S.A\nCNPJ 30.306.294/0001-45\ncartao final 5115";

    // ── arquivo e extração ─────────────────────────────────────────────────────

    @Test
    void semArquivoRetorna422() {
        asMultipartUser()
                .multiPart("password", "irrelevante")
                .when().post(path(PREVIEW))
                .then().statusCode(422)
                .body("code", is("FILE_REQUIRED"));
    }

    @Test
    void pdfCriptografadoSemSenhaRetorna422PedindoASenha() throws IOException {
        preview(PdfFixtures.encrypted("segredo", FATURA_BTG))
                .statusCode(422)
                .body("code", is("PASSWORD_REQUIRED"));
    }

    @Test
    void pdfCriptografadoComSenhaErradaRetorna422() throws IOException {
        preview(PdfFixtures.encrypted("segredo", FATURA_BTG), "errada")
                .statusCode(422)
                .body("code", is("WRONG_PASSWORD"));
    }

    @Test
    void pdfCriptografadoComSenhaCorretaEhLidoNormalmente() throws IOException {
        preview(PdfFixtures.encrypted("segredo", FATURA_BTG), "segredo")
                .statusCode(200)
                .body("issuer", is("BTG Pactual"));
    }

    @Test
    void pdfSemCamadaDeTextoRetorna422() throws IOException {
        preview(PdfFixtures.imageOnly())
                .statusCode(422)
                .body("code", is("NO_TEXT_LAYER"));
    }

    @Test
    void pdfComPaginasDemaisRetorna422() throws IOException {
        // O extrator recusa acima de 50 páginas, antes de tentar ler o texto.
        preview(PdfFixtures.withText(FATURA_BTG, 51))
                .statusCode(422)
                .body("code", is("TOO_MANY_PAGES"));
    }

    @Test
    void arquivoAcimaDoLimiteRetorna413() {
        // O limite da fatia é 10 MB; o backstop do container (20 MB) só existe para o pedido nem chegar.
        preview(new byte[11 * 1024 * 1024])
                .statusCode(413)
                .body("code", is("FILE_TOO_LARGE"));
    }

    // ── roteamento do documento ────────────────────────────────────────────────

    @Test
    void roteiaAsFaturasDeCartao() throws IOException {
        preview(fixturePdf("/faturas/fatura-btg-abril.txt")).statusCode(200)
                .body("documentType", is("CREDIT_CARD_INVOICE")).body("issuer", is("BTG Pactual"));
        preview(fixturePdf("/faturas/fatura-btg-maio.txt")).statusCode(200)
                .body("documentType", is("CREDIT_CARD_INVOICE")).body("issuer", is("BTG Pactual"));
        preview(fixturePdf("/faturas/fatura-santander-abril.txt")).statusCode(200)
                .body("documentType", is("CREDIT_CARD_INVOICE")).body("issuer", is("Santander"));
        preview(fixturePdf("/faturas/fatura-santander-maio.txt")).statusCode(200)
                .body("documentType", is("CREDIT_CARD_INVOICE")).body("issuer", is("Santander"));
    }

    @Test
    void roteiaOsExtratosDeContaCorrente() throws IOException {
        preview(fixturePdf("/extratos/extrato-btg-2025.txt")).statusCode(200)
                .body("documentType", is("BANK_STATEMENT")).body("issuer", is("BTG Pactual"));
        preview(fixturePdf("/extratos/extrato-santander-202512.txt")).statusCode(200)
                .body("documentType", is("BANK_STATEMENT")).body("issuer", is("Santander"));
    }

    @Test
    void marcadorDeExtratoVenceONomeDoBancoContraparte() throws IOException {
        // Extrato Santander que cita "BTG Pactual" como contraparte de um boleto continua Santander.
        preview(PdfFixtures.withText(String.join("\n",
                "EXTRATO CONSOLIDADO INTELIGENTE",
                "11/12 Pagamento de boleto Fatura Cartao BTG Pactual -3.076,08")))
                .statusCode(200)
                .body("documentType", is("BANK_STATEMENT"))
                .body("issuer", is("Santander"));
    }

    @Test
    void cnpjVenceONomeDoBancoContraparte() throws IOException {
        // Extrato de conta corrente BTG (CNPJ …0002-26) que pagou um boleto ao "Santander".
        preview(PdfFixtures.withText(String.join("\n",
                "Extrato BTG Pactual - CNPJ 30.306.294/0002-26",
                "17/02/2025 13h11 Pagamento de boleto Santander -R$ 1.212,32")))
                .statusCode(200)
                .body("documentType", is("BANK_STATEMENT"))
                .body("issuer", is("BTG Pactual"));
    }

    @Test
    void detectaExtratoBtgSoPeloCabecalho() throws IOException {
        preview(PdfFixtures.withText("Este é o extrato da sua conta corrente BTG Pactual"))
                .statusCode(200)
                .body("documentType", is("BANK_STATEMENT"))
                .body("issuer", is("BTG Pactual"));
    }

    @Test
    void bancoDesconhecidoRetorna422ComMensagemClara() throws IOException {
        preview(PdfFixtures.withText("Documento qualquer sem marcador de banco"))
                .statusCode(422)
                .body("code", is("UNKNOWN_ISSUER"))
                .body("detail", containsString("não reconhecido"));
    }
}
