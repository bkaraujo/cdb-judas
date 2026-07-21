package br.cdb.feature.f006._0_domain;

import org.jspecify.annotations.NullMarked;

/**
 * Typed failures of a credit-card statement import. Each carries a stable {@link #code()} the
 * frontend maps to UX (e.g. revealing the password field) and a user-facing pt-BR {@link #message()}.
 */
@NullMarked
public sealed interface ImportError {

    String code();

    String message();

    @NullMarked
    record PasswordRequired() implements ImportError {
        @Override public String code() { return "PASSWORD_REQUIRED"; }
        @Override public String message() { return "Este PDF está protegido. Informe a senha para continuar."; }
    }

    @NullMarked
    record WrongPassword() implements ImportError {
        @Override public String code() { return "WRONG_PASSWORD"; }
        @Override public String message() { return "Senha incorreta. Verifique e tente novamente."; }
    }

    @NullMarked
    record NoTextLayer() implements ImportError {
        @Override public String code() { return "NO_TEXT_LAYER"; }
        @Override public String message() {
            return "Não foi possível ler o texto do PDF. Envie o arquivo original do banco, não uma imagem digitalizada.";
        }
    }

    @NullMarked
    record AccountNotFound() implements ImportError {
        @Override public String code() { return "ACCOUNT_NOT_FOUND"; }
        @Override public String message() { return "Conta não encontrada."; }
    }

    @NullMarked
    record UnknownIssuer() implements ImportError {
        @Override public String code() { return "UNKNOWN_ISSUER"; }
        @Override public String message() {
            return "Banco não reconhecido. Esta versão suporta apenas faturas Santander e BTG Pactual.";
        }
    }

    @NullMarked
    record FileTooLarge(long sizeBytes, long maxBytes) implements ImportError {
        @Override public String code() { return "FILE_TOO_LARGE"; }
        @Override public String message() {
            return "Arquivo muito grande. O limite é de " + (maxBytes / (1024 * 1024)) + " MB.";
        }
    }

    @NullMarked
    record TooManyPages(int pages, int maxPages) implements ImportError {
        @Override public String code() { return "TOO_MANY_PAGES"; }
        @Override public String message() {
            return "PDF com páginas demais (" + pages + "). O limite é de " + maxPages + " páginas.";
        }
    }
}
