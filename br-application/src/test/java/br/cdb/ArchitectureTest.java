package br.cdb;

import br.commons.framework.persistence.jdbc.JDBCRepository;
import br.commons.tools.Meta;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.ws.rs.Path;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "br",
        importOptions = ImportOption.DoNotIncludeTests.class
)
@NullMarked
class ArchitectureTest {
    @ArchTest
    static final ArchRule all_classes_must_be_null_marked =
            classes().that().areNotEnums()
                    .and().areNotAnonymousClasses()
                    .and().haveSimpleNameNotEndingWith("Builder")
                    .and().resideInAPackage("br..")
                    .should().beAnnotatedWith(NullMarked.class)
                    .because("todo tipo deve declarar explicitamente seu contrato de nullability");

    @ArchTest
    static final ArchRule resources_must_live_in_infrastructure = classes()
            .that().areAnnotatedWith(Path.class)
            .should().resideInAnyPackage(".._2_infrastructure.web");

    @ArchTest
    static final ArchRule resources_request_must_live_in_infrastructure = classes()
            .that().areRecords()
            .and().haveSimpleNameEndingWith("Request")
            .should().resideInAnyPackage(".._2_infrastructure.web.request");
    @ArchTest
    static final ArchRule resources_response_must_live_in_infrastructure = classes()
            .that().areRecords()
            .and().haveSimpleNameEndingWith("Response")
            .should().resideInAnyPackage(".._2_infrastructure.web.response");

    @ArchTest
    static final ArchRule resources_must_not_access_repositories =
            noClasses().that().haveSimpleNameEndingWith("Resource")
                    .and().haveSimpleNameNotEndingWith("LoginResource")
                    .and().haveSimpleNameNotEndingWith("SelfResource")
                    .should().accessClassesThat().haveSimpleNameEndingWith("Repository")
                    .because("LoginResource e SelfResource são exceções deliberadas: acessam UserRepository direto (equivalente ao UserDetailsService do Spring) para autenticação/username de login, sem Facade de contexto para isso");


    @ArchTest
    static final ArchRule jdbcrepository_must_live_in_infrastructure = classes()
            .that().areAssignableTo(JDBCRepository.class)
            .and().doNotHaveFullyQualifiedName(Meta.fqn(JDBCRepository.class))
            .should().resideInAnyPackage(".._2_infrastructure.persistence");

    @ArchTest
    static final ArchRule core_must_not_access_feature =
            noClasses().that().resideInAPackage("..core..")
                    .and().resideOutsideOfPackage("..feature..")
                    .should(notAccessFeatureExceptDissolvedRemnants())
                    .because("o core da aplicação não deve depender de features; cores locais de feature (feature..core) são exceção legítima");

    @ArchTest
    static final ArchRule application_must_not_access_infrastructure =
            noClasses().that().resideInAPackage(".._1_application..")
                    .should().accessClassesThat().resideInAPackage(".._2_infrastructure..")
                    .because("serviços de aplicação devem depender de abstrações de domínio (_0_domain), não de implementações de infraestrutura");


    static final ArchRule feature_slices_depend_only_on_earlier_ones =
            classes().that().resideInAPackage("..feature..")
                    .should(dependOnlyOnEarlierFeatureSlices())
                    .because("fNNN só consome fMMM com MMM < NNN (f000 é base); dependência 'para cima' inverte-se via porta na fatia anterior — ver CLAUDE.md");

    @ArchTest
    static final ArchRule feature_slices_must_not_depend_on_sibling_slices =
            classes().that().resideInAPackage("..feature..")
                    .should(notDependOnSiblingFeatureSlices())
                    .because("fatia de negócio não importa fatia de negócio irmã; use evento (f000), porta no _0_domain do consumidor, ou adapter em f999 — ver CLAUDE.md");

    @ArchTest
    static final ArchRule person_scoped_repositories_must_declare_a_person_scoped_finder =
            classes().that().haveSimpleName("AccountRepository")
                    .or().haveSimpleName("CreditCardRepository")
                    .or().haveSimpleName("TransactionRepository")
                    .and().resideInAPackage("..feature.._0_domain.repository..")
                    .should(declareAPersonScopedFinder())
                    .because("guarda implícita: toda tabela de fatia tem COD_PERSON — ver javadoc da regra");

    private static final Pattern FEATURE_NUMBER = Pattern.compile("\\.feature\\.f(\\d+)(\\.|$)");

    private static int featureNumber(JavaClass clazz) {
        val matcher = FEATURE_NUMBER.matcher("." + clazz.getPackageName() + ".");
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    /**
     * Marca um alvo como remanescente dos contextos dissolvidos na fase 2 — sempre um dos
     * subpacotes que só existem porque preservam a organização interna que a classe já tinha em
     * {@code br-context-monetary}/{@code br-context-people} ({@code _0_domain.model/repository/
     * event}, {@code _1_application.command/service/usecase/event}). Nenhuma fatia nativa usa esses
     * nomes de subpacote hoje — não crie um novo com esses nomes fora da dissolução, ou ele também
     * cairia nesta exceção sem querer.
     */
    private static boolean isDissolvedContextRemnant(JavaClass clazz) {
        val pkg = "." + clazz.getPackageName() + ".";
        return pkg.contains("._0_domain.model.")
                || pkg.contains("._0_domain.repository.")
                || pkg.contains("._0_domain.event.")
                || pkg.contains("._1_application.command.")
                || pkg.contains("._1_application.service.")
                || pkg.contains("._1_application.usecase.")
                || pkg.contains("._1_application.event.");
    }

    private static ArchCondition<JavaClass> dependOnlyOnEarlierFeatureSlices() {
        return new ArchCondition<>("depender apenas de fatias fNNN anteriores (número menor; f000 é base)") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                val from = featureNumber(origin);
                if (from < 0) {
                    return; // classe fora de uma fatia fNNN (ex.: package-info da raiz feature)
                }
                for (val dependency : origin.getDirectDependenciesFromSelf()) {
                    val target = dependency.getTargetClass();
                    val to = featureNumber(target);
                    if (to <= from) {
                        continue;
                    }
                    if (isDissolvedContextRemnant(target)) {
                        continue; // exceção temporária desde a fase 2 — ver javadoc da regra 6 e de isDissolvedContextRemnant
                    }
                    events.add(SimpleConditionEvent.violated(dependency,
                            "f%03d depende de f%03d (fatia posterior): %s"
                                    .formatted(from, to, dependency.getDescription())));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notAccessFeatureExceptDissolvedRemnants() {
        return new ArchCondition<>("não acessar pacote ..feature.. (exceto remanescente dos contextos dissolvidos, temporário fase 2→4)") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (val dependency : origin.getDirectDependenciesFromSelf()) {
                    val target = dependency.getTargetClass();
                    if (featureNumber(target) < 0) {
                        continue; // não é classe de fNNN
                    }
                    if (isDissolvedContextRemnant(target)) {
                        continue; // exceção temporária desde a fase 2 — ver javadoc da regra e de isDissolvedContextRemnant
                    }
                    events.add(SimpleConditionEvent.violated(dependency,
                            "core depende de feature: " + dependency.getDescription()));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> declareAPersonScopedFinder() {
        return new ArchCondition<>("declarar pelo menos um finder escopado por pessoa (nome contendo \"Person\")") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                val hasPersonScopedFinder = clazz.getMethods().stream()
                        .anyMatch(m -> m.getName().contains("Person"));
                if (!hasPersonScopedFinder) {
                    events.add(SimpleConditionEvent.violated(clazz,
                            clazz.getFullName() + " não declara nenhum finder escopado por pessoa (nome contendo \"Person\")"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnSiblingFeatureSlices() {
        return new ArchCondition<>("não depender de fatia fNNN irmã (nem f000 nem f999 na origem)") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                val from = featureNumber(origin);
                if (from < 0) {
                    return; // classe fora de uma fatia fNNN (ex.: package-info da raiz feature)
                }
                if (from == 999) {
                    return; // f999 é o composition root: pode importar todas
                }
                for (val dependency : origin.getDirectDependenciesFromSelf()) {
                    val target = dependency.getTargetClass();
                    val to = featureNumber(target);
                    if (to < 0 || to == 0 || to == from) {
                        continue; // fora de fNNN, alvo f000 (kernel) ou mesma fatia: permitido
                    }
                    if (isDissolvedContextRemnant(target)) {
                        continue; // exceção temporária desde a fase 2 — ver javadoc da regra e de isDissolvedContextRemnant
                    }
                    events.add(SimpleConditionEvent.violated(dependency,
                            "f%03d depende de f%03d (fatia irmã): %s"
                                    .formatted(from, to, dependency.getDescription())));
                }
            }
        };
    }

}
