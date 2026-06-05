package br.community.context.monetary;

import br.community.feature.user.accounts.statement.importer.provider.BtgCreditCardStatementParser;
import br.community.feature.user.accounts.statement.importer.preview.CreditCardStatementParserRegistry;
import br.community.feature.user.accounts.statement.importer.preview.Issuer;
import br.community.feature.user.accounts.statement.importer.provider.SantanderCreditCardStatementParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditCardStatementParserRegistryTest {

    private final CreditCardStatementParserRegistry registry = new CreditCardStatementParserRegistry(
            new SantanderCreditCardStatementParser(), new BtgCreditCardStatementParser());

    @Test
    void resolvesSantanderToSantanderParser() {
        assertInstanceOf(SantanderCreditCardStatementParser.class, registry.forIssuer(Issuer.SANTANDER).orElseThrow());
    }

    @Test
    void resolvesBtgToBtgParser() {
        assertInstanceOf(BtgCreditCardStatementParser.class, registry.forIssuer(Issuer.BTG).orElseThrow());
    }

    @Test
    void hasNoParserForUnknownIssuer() {
        assertTrue(registry.forIssuer(Issuer.UNKNOWN).isEmpty());
    }
}
