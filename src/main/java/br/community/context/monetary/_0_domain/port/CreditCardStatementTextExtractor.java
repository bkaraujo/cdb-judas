package br.community.context.monetary._0_domain.port;

import br.commons.Result;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Driven port: turns raw PDF bytes (optionally password-protected) into statement text.
 * The single owner of the PDF-parsing technology lives in {@code _2_infrastructure}.
 */
@NullMarked
public interface CreditCardStatementTextExtractor {

    Result<String, ExtractionFailure> extract(byte[] bytes, @Nullable String password);
}
