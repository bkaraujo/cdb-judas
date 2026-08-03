package br.cdb;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * A suíte de {@code br.cdb} só pode ter três tipos de teste: <b>(1)</b> teste de use case
 * ({@code *UseCaseTest}/{@code *UseCasesTest}, sobre {@code AbstractUseCaseTest}, com repositórios em
 * memória), <b>(2)</b> teste de interface Web (herda {@link BaseHttpTest} e fala com a fatia só por
 * HTTP) e <b>(3)</b> teste de arquitetura ({@code *ArchitectureTest}, ArchUnit). Não há teste de
 * unidade de classe interna — parser, expansor, casador e serviço são verificados pelo que a borda
 * expõe.
 *
 * <p><b>Exceção por papel arquitetural: {@code f999}.</b> O composition root não tem borda nenhuma —
 * nem rota HTTP nem use case — e ainda assim carrega comportamento crítico que só roda em processo (a
 * fila de retry de exclusão, {@code DeletionQueueService.runOnce()} no {@code @Scheduled}). Como não
 * há por onde alcançá-lo de fora, teste de unidade ali é a única forma possível e fica permitido. Não
 * é licença para voltar a testar classe interna de fatia com borda.
 *
 * <p>É uma trava estrutural, não semântica: garante que nenhuma classe de teste nova apareça fora dos
 * três formatos e que um teste Web não contorne o HTTP injetando o serviço da fatia, mas não sabe
 * dizer se um teste Web de fato exercita uma rota. Precisa da própria importação de classes (o
 * {@code ArchitectureTest} analisa o código de produção, com {@code DoNotIncludeTests}).
 */
@NullMarked
class TestSuiteArchitectureTest {

    private static final String JUNIT_TEST = "org.junit.jupiter.api.Test";
    private static final String CDI_INJECT = "jakarta.inject.Inject";

    private static JavaClasses testClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages("br.cdb");
    }

    @Test
    void toda_classe_de_teste_e_de_use_case_de_interface_web_ou_de_arquitetura() {
        classes().that().resideInAPackage("br.cdb..")
                .should(beOneOfTheThreeAllowedKinds())
                .because("a suíte só tem teste de use case, de interface Web e de ArchUnit — ver javadoc")
                .check(testClasses());
    }

    @Test
    void teste_de_interface_web_nao_injeta_servico_de_fatia() {
        classes().that().areAssignableTo(BaseHttpTest.class)
                .should(notInjectSliceBeans())
                .because("teste de interface Web fala com a fatia por HTTP; injetar o serviço é testá-lo por dentro")
                .check(testClasses());
    }

    private static ArchCondition<JavaClass> beOneOfTheThreeAllowedKinds() {
        return new ArchCondition<>("ser *UseCaseTest, herdar BaseHttpTest ou ser *ArchitectureTest (f999 isento)") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                if (clazz.getMethods().stream().noneMatch(m -> m.isAnnotatedWith(JUNIT_TEST))) {
                    return; // classe de apoio (fixture, builder de dado): não é teste
                }
                if (clazz.getPackageName().contains(".feature.f999")) {
                    return; // composition root: sem rota e sem use case, não há borda por onde testar
                }
                val name = clazz.getSimpleName();
                if (name.endsWith("UseCaseTest") || name.endsWith("UseCasesTest")
                        || name.endsWith("ArchitectureTest")
                        || clazz.isAssignableTo(BaseHttpTest.class)) {
                    return;
                }
                events.add(SimpleConditionEvent.violated(clazz,
                        "%s não é teste de use case, de interface Web nem de arquitetura".formatted(clazz.getFullName())));
            }
        };
    }

    private static ArchCondition<JavaClass> notInjectSliceBeans() {
        return new ArchCondition<>("não injetar bean de _1_application de uma fatia") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (val field : clazz.getFields()) {
                    if (!field.isAnnotatedWith(CDI_INJECT)) {
                        continue;
                    }
                    val injected = field.getRawType().getPackageName();
                    if (injected.contains("._1_application")) {
                        events.add(SimpleConditionEvent.violated(field,
                                "%s injeta %s em vez de chamar a rota".formatted(clazz.getFullName(), injected)));
                    }
                }
            }
        };
    }
}
