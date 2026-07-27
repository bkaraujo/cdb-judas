package br.cdb.feature.f006._1_application;

import br.cdb.context.monetary.MonetaryUseCases;
import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.cdb.feature.f005._1_application.UserTransactionService;
import br.cdb.feature.f006._0_domain.CreditCardProvider;
import br.cdb.feature.f006._0_domain.StatementParser;
import br.commons.pdf.PdfTextExtractor;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.time.Clock;
import java.util.List;

@NullMarked
@RequiredArgsConstructor
abstract class AbstractMonetaryDocumentImporter {

    protected static final int RECONCILE_WINDOW_DAYS = 3;

    protected final AccountUseCase ucAccount = MonetaryUseCases.ucAccount();
    protected final TransactionUseCase ucTransaction = MonetaryUseCases.ucTransaction();

    protected final CreditCardProvider creditCardProvider;
    protected final PdfTextExtractor extractor;
    protected final List<StatementParser> parsers;
    protected final CardMatcher cardMatcher = new CardMatcher();
    protected final InstallmentExpander expander;
    protected final GroupSignature groupSignature = new GroupSignature();
    protected final CategoryGuesser categoryGuesser = new CategoryGuesser();
    protected final UserTransactionService userTransactionService;
    protected final Clock clock;
}
