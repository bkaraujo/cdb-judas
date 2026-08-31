package br.cdb.feature.f010;

import br.cdb.AbstractUseCaseTest;
import br.cdb.feature.f010._0_domain.model.ImportRule;
import br.cdb.feature.f010._1_application.usecase.ReadUseCase;
import br.cdb.feature.f010._1_application.usecase.WriteUseCase;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cobre a leitura de regra de nomenclatura da fatia {@code f010} — o par de {@code WriteUseCaseTest}.
 * Aqui o fake <em>modela</em> {@code COD_PERSON} (personId é campo de {@link ImportRule}), então a
 * guarda implícita por pessoa é exercitável fora do HTTP.
 */
class ReadUseCaseTest extends AbstractUseCaseTest {

    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID OTHER_PERSON_ID = UUID.randomUUID();

    private ReadUseCase reads;

    @BeforeEach
    void setUp() {
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        reads = new ReadUseCase();
    }

    private ImportRule seedRule(UUID personId, String name) {
        val ruleId = UUID.randomUUID();
        val rule = importRuleRepository().save(new ImportRule(ruleId, personId, name, List.of(), null, null, null, null));
        importRuleTriggerRepository().replaceTriggers(ruleId, personId, List.of(name));
        // Gravar no repositório não emite evento: o cache off-heap só enxerga o que o populate leu.
        populateCacheFor(PERSON_ID);
        return rule;
    }

    @Test
    @DisplayName("rules devolve só as regras da pessoa")
    void listsRulesOfPerson() {
        seedRule(PERSON_ID, "Companhia de Saneamento");
        seedRule(PERSON_ID, "Netflix");
        seedRule(OTHER_PERSON_ID, "De outro dono");

        val rules = reads.rules(PERSON_ID);

        assertEquals(List.of("Companhia de Saneamento", "Netflix"), rules.stream().map(ImportRule::name).toList());
    }

    @Test
    @DisplayName("rule devolve a regra da pessoa")
    void findsRuleOfPerson() {
        val rule = seedRule(PERSON_ID, "Netflix");

        val r = reads.rule(PERSON_ID, rule.id());

        assertTrue(r.isSuccess());
        assertEquals(rule.id(), ((Result.Success<ImportRule, BusinessError>) r).value().id());
    }

    @Test
    @DisplayName("rule de outra pessoa → NotFound (guarda implícita por COD_PERSON)")
    void hidesRuleOfAnotherPerson() {
        val rule = seedRule(OTHER_PERSON_ID, "De outro dono");

        val r = reads.rule(PERSON_ID, rule.id());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<ImportRule, BusinessError>) r).error());
    }

    @Test
    @DisplayName("rule inexistente → NotFound")
    void findUnknownRule() {
        val r = reads.rule(PERSON_ID, UUID.randomUUID());
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<ImportRule, BusinessError>) r).error());
    }
}
