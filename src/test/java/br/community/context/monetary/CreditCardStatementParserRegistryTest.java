package br.community.context.monetary;

import br.community.context.monetary._0_domain.model.Issuer;
import br.community.context.monetary._1_application.service.BtgCreditCardStatementParser;
import br.community.context.monetary._1_application.service.CreditCardStatementParserRegistry;
import br.community.context.monetary._1_application.service.SantanderCreditCardStatementParser;
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
