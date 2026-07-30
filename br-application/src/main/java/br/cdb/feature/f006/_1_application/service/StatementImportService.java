package br.cdb.feature.f006._1_application.service;

import br.cdb.feature.f006._0_domain.*;
import br.cdb.feature.f006._1_application.confirm.InvoiceConfirmCommand;
import br.cdb.feature.f006._1_application.confirm.StatementConfirmCommand;
import br.cdb.feature.f006._1_application.preview.ImportPreviewOutcome;
import br.cdb.feature.f006._1_application.preview.InvoiceImportProcessor;
import br.cdb.feature.f006._1_application.preview.StatementImportProcessor;
import br.commons.Logger;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.pdf.ExtractionFailure;
import br.commons.pdf.PdfTextExtractor;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates credit-card statement import against the monetary context use cases: extracts and
 * parses the PDF, then routes the parsed {@link MonetaryDocument} to the matching specialized
 * processor — {@link InvoiceImportProcessor} for card invoices, {@link StatementImportProcessor}
 * for checking-account extracts.
 */
@NullMarked
public class StatementImportService {

    private final PdfTextExtractor extractor;
    private final List<StatementParser> parsers;
    private final long maxFileBytes;
    private final InvoiceImportProcessor invoiceProcessor;
    private final StatementImportProcessor statementProcessor;

    public StatementImportService(CreditCardProvider creditCardProvider, PdfTextExtractor extractor, List<StatementParser> parsers, long bytes) {
        this(creditCardProvider, extractor, parsers, bytes, Clock.system(ZoneId.systemDefault()));
    }

    public StatementImportService(CreditCardProvider creditCardProvider, PdfTextExtractor extractor, List<StatementParser> parsers, long bytes, Clock clock) {
        this.extractor = extractor;
        this.parsers = parsers;
        this.maxFileBytes = bytes;
        this.invoiceProcessor = new InvoiceImportProcessor(creditCardProvider, clock);
        this.statementProcessor = new StatementImportProcessor(clock);
    }

    public Result<ImportPreviewOutcome, ImportError> preview(String personId, byte[] fileBytes, @Nullable String password, @Nullable UUID accountId) {
        if (fileBytes.length > maxFileBytes) {
            return new Result.Failure<>(new ImportError.FileTooLarge(fileBytes.length, maxFileBytes));
        }

        Logger.trace("Processing %s bytes", fileBytes.length);
        return switch (extractor.extract(fileBytes, password)) {

            case Result.Success(var text) -> {
                if (text == null) yield new Result.Failure<>(new ImportError.NoTextLayer());
                Logger.verbose("Extracted %s characters", text.length());

                val capable = parsers.stream().filter(parser -> parser.parseable(text)).toList();
                if (capable.size() != 1) {
                    yield new Result.Failure<>(new ImportError.UnknownIssuer());
                }

                yield switch (capable.getFirst().parse(text)) {
                    case MonetaryDocument.Invoice(var issuer, var period, var statement) -> invoiceProcessor.preview(personId, issuer, period, statement);
                    case MonetaryDocument.Statement(var issuer, var statement) -> statementProcessor.preview(personId, issuer, statement, accountId);
                };
            }

            case Result.Failure(var failure) -> new Result.Failure<>(switch ((ExtractionFailure) failure) {
                case ExtractionFailure.Encrypted ignored -> new ImportError.PasswordRequired();
                case ExtractionFailure.WrongPassword ignored -> new ImportError.WrongPassword();
                case ExtractionFailure.NoTextLayer ignored -> new ImportError.NoTextLayer();
                case ExtractionFailure.TooManyPages(int pages, int maxPages) -> new ImportError.TooManyPages(pages, maxPages);
            });
        };
    }

    public Result<ImportResult, BusinessError> confirm(UUID personId, InvoiceConfirmCommand cmd) {
        return invoiceProcessor.confirm(personId, cmd);
    }

    public Result<ImportResult, BusinessError> confirm(UUID personId, StatementConfirmCommand cmd) {
        return statementProcessor.confirmStatement(personId, cmd);
    }
}
