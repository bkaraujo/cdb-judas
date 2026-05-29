package br.community.feature.user.accounts.statementimport;

import br.commons.pdf.PdfBoxTextExtractor;
import br.commons.pdf.PdfTextExtractor;
import br.community.context.monetary.MonetaryContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the credit-card statement-import feature: the PDF extractor (technical util), the parsing
 * services and the orchestrating {@link StatementImportUseCase}, which reaches the monetary context
 * only through its {@link MonetaryContext} facade.
 */
@Configuration
@NullMarked
public class StatementImportModule {

    private static final int MAX_STATEMENT_PAGES = 50;
    private static final long MAX_STATEMENT_FILE_BYTES = 10L * 1024 * 1024;

    @Bean
    PdfTextExtractor pdfTextExtractor() {
        return new PdfBoxTextExtractor(MAX_STATEMENT_PAGES);
    }

    @Bean
    IssuerDetector issuerDetector() {
        return new IssuerDetector();
    }

    @Bean
    CreditCardStatementParserRegistry creditCardStatementParserRegistry() {
        return new CreditCardStatementParserRegistry(
                new SantanderCreditCardStatementParser(), new BtgCreditCardStatementParser());
    }

    @Bean
    CardMatcher cardMatcher() {
        return new CardMatcher();
    }

    @Bean
    GroupSignature groupSignature() {
        return new GroupSignature();
    }

    @Bean
    InstallmentExpander installmentExpander(GroupSignature groupSignature) {
        return new InstallmentExpander(groupSignature);
    }

    @Bean
    CategoryGuesser categoryGuesser() {
        return new CategoryGuesser();
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    StatementImportUseCase statementImportUseCase(
            MonetaryContext monetaryContext,
            PdfTextExtractor extractor,
            IssuerDetector issuerDetector,
            CreditCardStatementParserRegistry parsers,
            CardMatcher cardMatcher,
            InstallmentExpander installmentExpander,
            GroupSignature groupSignature,
            CategoryGuesser categoryGuesser,
            Clock clock) {
        return new StatementImportUseCase(
                monetaryContext, extractor, issuerDetector, parsers, cardMatcher,
                installmentExpander, groupSignature, categoryGuesser, clock, MAX_STATEMENT_FILE_BYTES);
    }
}
