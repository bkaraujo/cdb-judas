package br.community.context.monetary._1_application.service;

import br.community.context.monetary._0_domain.model.Issuer;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Optional;

/** Resolves a detected {@link Issuer} to its {@link CreditCardStatementParser}. */
@NullMarked
public class CreditCardStatementParserRegistry {

    private final Map<Issuer, CreditCardStatementParser> byIssuer;

    public CreditCardStatementParserRegistry(
            SantanderCreditCardStatementParser santander,
            BtgCreditCardStatementParser btg) {
        this.byIssuer = Map.of(
                Issuer.SANTANDER, santander,
                Issuer.BTG, btg);
    }

    public Optional<CreditCardStatementParser> forIssuer(Issuer issuer) {
        return Optional.ofNullable(byIssuer.get(issuer));
    }
}
