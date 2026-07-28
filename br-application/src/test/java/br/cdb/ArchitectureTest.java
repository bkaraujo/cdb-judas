package br.cdb;

import br.commons.annotation.Facade;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.regex.Pattern;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.implement;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "br",
        importOptions = ImportOption.DoNotIncludeTests.class
)
@NullMarked
class ArchitectureTest {

    @ArchTest
    static final ArchRule resources_must_not_access_repositories =
            noClasses().that().haveSimpleNameEndingWith("Resource")
                    .and().haveSimpleNameNotEndingWith("LoginResource")
                    .and().haveSimpleNameNotEndingWith("SelfResource")
                    .should().accessClassesThat().haveSimpleNameEndingWith("Repository")
                    .because("LoginResource e SelfResource são exceções deliberadas: acessam UserRepository direto (equivalente ao UserDetailsService do Spring) para autenticação/username de login, sem Facade de contexto para isso");

    @ArchTest
    static final ArchRule all_classes_must_be_null_marked =
            classes().that().areNotEnums()
                    .and().areNotAnonymousClasses()
                    .and().haveSimpleNameNotEndingWith("Builder")
                    .and().resideInAPackage("br..")
                    .should().beAnnotatedWith(NullMarked.class)
                    .because("todo tipo deve declarar explicitamente seu contrato de nullability");

    @ArchTest
    static final ArchRule core_must_not_access_feature =
            noClasses().that().resideInAPackage("..core..")
                    .and().resideOutsideOfPackage("..feature..")
                    .should().accessClassesThat().resideInAPackage("..feature..")
                    .because("o core da aplicação não deve depender de features; cores locais de feature (feature..core) são exceção legítima");

    @ArchTest
    static final ArchRule application_must_not_access_infrastructure =
            noClasses().that().resideInAPackage(".._1_application..")
                    .should().accessClassesThat().resideInAPackage(".._2_infrastructure..")
                    .because("serviços de aplicação devem depender de abstrações de domínio (_0_domain), não de implementações de infraestrutura");

    @ArchTest
    static final ArchRule feature_must_access_context_only_via_facade_or_domain_model =
            noClasses().that().resideInAPackage("..feature..")
                    .should().accessClassesThat(contextClassNotExposedViaFacade())
                    .because("feature deve acessar context exclusivamente via Facade, use cases (_1_application.usecase, obtidos pela Facade), modelos de domínio (_0_domain.model) ou eventos de domínio (_0_domain.event)");

    @ArchTest
    static final ArchRule context_must_not_depend_on_framework =
            noClasses().that().resideInAPackage("..context..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta..", "io.quarkus..")
                    .because("o contexto é livre de framework: DI via Registry, validação na borda (@Valid nos *HTTPRequest)");

    /**
     * O número da fatia expressa ordem de criação: uma feature {@code fNNN} só pode consumir recursos
     * de fatias que já existiam antes dela — {@code fMMM} com {@code MMM < NNN}. {@code f000} é a base
     * (não depende de feature nenhuma). Ex.: {@code f006} pode consumir {@code f002}/{@code f004}/
     * {@code f005}, mas nunca {@code f007}. A inversão de dependência resolve os casos em que uma fatia
     * anterior precisa de serviço de uma posterior: a anterior define a porta ({@code _0_domain}) e a
     * posterior a implementa (ver {@code AccountOwnership}/{@code TransactionAccountOverlay}/
     * {@code TransactionCategoryOverlay}).
     */
    @ArchTest
    static final ArchRule feature_slices_depend_only_on_earlier_ones =
            classes().that().resideInAPackage("..feature..")
                    .should(dependOnlyOnEarlierFeatureSlices())
                    .because("fNNN só consome fMMM com MMM < NNN (f000 é base); dependência 'para cima' inverte-se via porta na fatia anterior — ver CLAUDE.md");

    /**
     * Fatia de negócio não pode depender de fatia de negócio irmã, nem "para baixo" (regra 7 já
     * cobre "para cima"). Duas exceções, ambas por papel arquitetural, não por número específico:
     * alvo {@code f000} (kernel compartilhado) sempre permitido; origem {@code f999} (composition
     * root — único lugar que liga porta a provedor via adapter) sempre permitida como origem. Toda
     * outra dependência cross-slice resolve-se por evento em {@code f000._0_domain.event}, por porta
     * declarada pelo consumidor em seu próprio {@code _0_domain}, ou por adapter em
     * {@code f999._2_infrastructure.adapter} — ver CLAUDE.md.
     */
    @ArchTest
    static final ArchRule feature_slices_must_not_depend_on_sibling_slices =
            classes().that().resideInAPackage("..feature..")
                    .should(notDependOnSiblingFeatureSlices())
                    .because("fatia de negócio não importa fatia de negócio irmã; use evento (f000), porta no _0_domain do consumidor, ou adapter em f999 — ver CLAUDE.md");

    private static final Pattern FEATURE_NUMBER = Pattern.compile("\\.feature\\.f(\\d+)(\\.|$)");

    private static int featureNumber(JavaClass clazz) {
        val matcher = FEATURE_NUMBER.matcher("." + clazz.getPackageName() + ".");
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
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
                    val to = featureNumber(dependency.getTargetClass());
                    if (to > from) {
                        events.add(SimpleConditionEvent.violated(dependency,
                                "f%03d depende de f%03d (fatia posterior): %s"
                                        .formatted(from, to, dependency.getDescription())));
                    }
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
                    val to = featureNumber(dependency.getTargetClass());
                    if (to < 0 || to == 0 || to == from) {
                        continue; // fora de fNNN, alvo f000 (kernel) ou mesma fatia: permitido
                    }
                    events.add(SimpleConditionEvent.violated(dependency,
                            "f%03d depende de f%03d (fatia irmã): %s"
                                    .formatted(from, to, dependency.getDescription())));
                }
            }
        };
    }

    private static DescribedPredicate<JavaClass> contextClassNotExposedViaFacade() {
        return resideInAPackage("..context..")
                .and(not(resideInAPackage("..context..shared..")))
                .and(not(resideInAPackage("..context.._0_domain.model..")))
                .and(not(resideInAPackage("..context.._0_domain.event..")))
                .and(not(resideInAPackage("..context.._1_application.command..")))
                .and(not(resideInAPackage("..context.._1_application.event..")))
                .and(not(resideInAPackage("..context.._1_application.usecase..")))
                .and(not(implement(Facade.class)));
    }

}
