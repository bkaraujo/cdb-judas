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
 * Cobre a mutação de regra de nomenclatura da fatia {@code f010} — o par de {@code ReadUseCaseTest}.
 * Sem evento de exclusão (nada reage — o casamento é 100% api-side); o foco aqui é a validação de
 * ambiguidade na criação/edição de gatilhos.
 */
class WriteUseCaseTest extends AbstractUseCaseTest {

    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID OTHER_PERSON_ID = UUID.randomUUID();

    private WriteUseCase useCase;

    @BeforeEach
    void setUp() {
        Context.remove(ReadUseCase.class);
        Context.remove(WriteUseCase.class);
        useCase = new WriteUseCase();
    }

    private ImportRule seedRule(UUID personId, String name, List<String> triggers) {
        val ruleId = UUID.randomUUID();
        val rule = importRuleRepository().save(new ImportRule(ruleId, personId, name, List.of(), null, null, null, null));
        importRuleTriggerRepository().replaceTriggers(ruleId, personId, triggers);
        return rule;
    }

    @Test
    @DisplayName("createRule persiste a regra da pessoa com gatilhos")
    void createsRule() {
        val accountId = UUID.randomUUID();
        val categoryId = UUID.randomUUID();
        val planned = true;

        val r = useCase.createRule(PERSON_ID, "Companhia de Saneamento", List.of("Companhia de Saneamento"), accountId, categoryId, planned);

        assertTrue(r.isSuccess());
        val rule = ((Result.Success<ImportRule, BusinessError>) r).value();
        assertEquals(PERSON_ID, rule.personId());
        assertEquals("Companhia de Saneamento", rule.name());
        assertEquals(List.of("Companhia de Saneamento"), rule.triggers());
        assertEquals(accountId, rule.accountId());
        assertEquals(categoryId, rule.categoryId());
        assertEquals(planned, rule.planned());
        assertTrue(importRuleRepository().findById(rule.id()).isPresent());
    }

    @Test
    @DisplayName("createRule só com nome — conta/categoria/flag ficam nulos")
    void createsRuleNameOnly() {
        val r = useCase.createRule(PERSON_ID, "Netflix", List.of("Netflix"), null, null, null);

        assertTrue(r.isSuccess());
        val rule = ((Result.Success<ImportRule, BusinessError>) r).value();
        assertNull(rule.accountId());
        assertNull(rule.categoryId());
        assertNull(rule.planned());
        assertEquals(List.of("Netflix"), rule.triggers());
    }

    @Test
    @DisplayName("createRule com gatilho substring de regra existente → Conflict")
    void rejectsAmbiguousCreate() {
        seedRule(PERSON_ID, "Companhia de Saneamento", List.of("Companhia de Saneamento"));

        val r = useCase.createRule(PERSON_ID, "Companhia (rótulo)", List.of("Companhia"), null, null, null);

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Conflict.class, ((Result.Failure<ImportRule, BusinessError>) r).error());
    }

    @Test
    @DisplayName("createRule com gatilho que contém gatilho de regra existente (direção oposta) → Conflict")
    void rejectsAmbiguousCreateOppositeDirection() {
        seedRule(PERSON_ID, "Água (rótulo)", List.of("Água"));

        val r = useCase.createRule(PERSON_ID, "Companhia de Água e Saneamento", List.of("Companhia de Água e Saneamento"), null, null, null);

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Conflict.class, ((Result.Failure<ImportRule, BusinessError>) r).error());
    }

    @Test
    @DisplayName("createRule ignora acento/caixa na comparação de ambiguidade de gatilhos")
    void ambiguityCheckIsAccentAndCaseInsensitive() {
        seedRule(PERSON_ID, "Água (rótulo)", List.of("AGUA"));

        val r = useCase.createRule(PERSON_ID, "Conta de Água", List.of("Conta de Água"), null, null, null);

        assertTrue(r.isFailure());
    }

    @Test
    @DisplayName("createRule não compara gatilhos com regras de outra pessoa")
    void ambiguityCheckIsScopedToPerson() {
        seedRule(OTHER_PERSON_ID, "Companhia de Saneamento", List.of("Companhia de Saneamento"));

        val r = useCase.createRule(PERSON_ID, "Companhia de Saneamento", List.of("Companhia de Saneamento"), null, null, null);

        assertTrue(r.isSuccess());
    }

    @Test
    @DisplayName("createRule com 2 gatilhos persiste ambos")
    void createsRuleWithMultipleTriggers() {
        val r = useCase.createRule(PERSON_ID, "Padaria (rótulo)", List.of("Padaria", "PDR SAO JOSE"), null, null, null);

        assertTrue(r.isSuccess());
        val rule = ((Result.Success<ImportRule, BusinessError>) r).value();
        assertEquals(List.of("Padaria", "PDR SAO JOSE"), rule.triggers());
    }

    @Test
    @DisplayName("updateRule troca os campos preservando o dono")
    void updatesRule() {
        val rule = seedRule(PERSON_ID, "Netflix", List.of("Netflix"));
        val categoryId = UUID.randomUUID();

        val r = useCase.updateRule(PERSON_ID, rule.id(), "Netflix Brasil", List.of("Netflix Brasil"), null, categoryId, null);

        assertTrue(r.isSuccess());
        val updated = ((Result.Success<ImportRule, BusinessError>) r).value();
        assertEquals("Netflix Brasil", updated.name());
        assertEquals(List.of("Netflix Brasil"), updated.triggers());
        assertEquals(categoryId, updated.categoryId());
        assertEquals(PERSON_ID, updated.personId());
    }

    @Test
    @DisplayName("updateRule não se autoconflita com seus próprios gatilhos")
    void updateDoesNotConflictWithItself() {
        val rule = seedRule(PERSON_ID, "Netflix", List.of("Netflix"));

        val r = useCase.updateRule(PERSON_ID, rule.id(), "Netflix", List.of("Netflix"), null, null, null);

        assertTrue(r.isSuccess());
    }

    @Test
    @DisplayName("updateRule ambíguo com outra regra → Conflict")
    void rejectsAmbiguousUpdate() {
        seedRule(PERSON_ID, "Companhia de Saneamento", List.of("Companhia de Saneamento"));
        val other = seedRule(PERSON_ID, "Netflix", List.of("Netflix"));

        val r = useCase.updateRule(PERSON_ID, other.id(), "Netflix", List.of("Companhia"), null, null, null);

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.Conflict.class, ((Result.Failure<ImportRule, BusinessError>) r).error());
    }

    @Test
    @DisplayName("updateRule de regra inexistente → NotFound")
    void updateUnknownRule() {
        val r = useCase.updateRule(PERSON_ID, UUID.randomUUID(), "X", List.of("X"), null, null, null);
        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<ImportRule, BusinessError>) r).error());
    }

    @Test
    @DisplayName("deleteRule apaga a regra e seus gatilhos")
    void deletesRule() {
        val rule = seedRule(PERSON_ID, "Netflix", List.of("Netflix"));

        val r = useCase.deleteRule(PERSON_ID, rule.id());

        assertTrue(r.isSuccess());
        assertTrue(importRuleRepository().findById(rule.id()).isEmpty());
        assertTrue(importRuleTriggerRepository().findTriggersByPerson(PERSON_ID).isEmpty());
    }

    @Test
    @DisplayName("deleteRule de outra pessoa → NotFound, regra preservada (guarda anti-IDOR)")
    void deleteRejectsRuleOfAnotherPerson() {
        val rule = seedRule(OTHER_PERSON_ID, "De outro dono", List.of("De outro dono"));

        val r = useCase.deleteRule(PERSON_ID, rule.id());

        assertTrue(r.isFailure());
        assertInstanceOf(BusinessError.NotFound.class, ((Result.Failure<Void, BusinessError>) r).error());
        assertTrue(importRuleRepository().findById(rule.id()).isPresent());
    }
}
