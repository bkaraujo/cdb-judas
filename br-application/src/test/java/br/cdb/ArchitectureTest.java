package br.cdb;

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

    /**
     * <strong>Exceção temporária (fase 2→4 de {@code .claude/plan.md}):</strong> alvo remanescente
     * dos contextos dissolvidos ({@link #isDissolvedContextRemnant}) é tolerado — {@code ContextBridge}
     * (core) precisa referenciar as portas de repositório ex-contexto (agora {@code fNNN._0_domain.repository})
     * para publicar os adaptadores JDBC no {@link br.commons.Registry} no startup, papel que já exercia
     * antes da dissolução sem violar esta regra (as portas não moravam em {@code ..feature..}).
     */
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

    /**
     * O número da fatia expressa ordem de criação: uma feature {@code fNNN} só pode consumir recursos
     * de fatias que já existiam antes dela — {@code fMMM} com {@code MMM < NNN}. {@code f000} é a base
     * (não depende de feature nenhuma). Ex.: {@code f006} pode consumir {@code f002}/{@code f004}/
     * {@code f005}, mas nunca {@code f007}. A inversão de dependência resolve os casos em que uma fatia
     * anterior precisa de serviço de uma posterior: a anterior define a porta ({@code _0_domain}) e a
     * posterior a implementa (ver {@code TransactionAccountOverlay}/{@code TransactionCategoryOverlay}).
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
     *
     * <p><strong>Exceção temporária (fase 2→4 de {@code .claude/plan.md}):</strong> alvo que é
     * remanescente dos contextos recém-dissolvidos ({@link #isDissolvedContextRemnant}) também é
     * tolerado, qualquer que seja a origem — os antigos usecases/services/models/repositories de
     * {@code br-context-monetary}/{@code br-context-people} viraram subpacotes dentro de
     * {@code fNNN} preservando a organização interna que já tinham como contexto, mas ainda são
     * chamados cross-slice do jeito antigo (ex.: {@code f004}/{@code f005} chamando
     * {@code f006.TransactionUseCase} direto, sem porta) até a fase 4 trocar esse acesso por
     * evento/HTTP interno. Enquanto essa exceção existir, "{@code mvn verify} verde" não significa
     * mais "zero acoplamento cross-slice" — só "zero acoplamento novo fora do que já veio da
     * dissolução".</p>
     */
    @ArchTest
    static final ArchRule feature_slices_must_not_depend_on_sibling_slices =
            classes().that().resideInAPackage("..feature..")
                    .should(notDependOnSiblingFeatureSlices())
                    .because("fatia de negócio não importa fatia de negócio irmã; use evento (f000), porta no _0_domain do consumidor, ou adapter em f999 — ver CLAUDE.md");

    /**
     * Guarda implícita (fase 3 de {@code .claude/plan.md}): {@code F002_ACCOUNT}/{@code F003_CARD}/
     * {@code F006_TRANSACTION} carregam {@code COD_PERSON} nativo — o repositório precisa declarar
     * pelo menos um finder que o use ({@code findAllByPerson}/{@code findByIdAndPerson}/
     * {@code findByAccountAndPerson}, todos com "Person" no nome), não só os herdados de
     * {@code Repository<T,ID>} ({@code findAll}/{@code findById}, sem pessoa — mantidos para uso
     * interno do engine, ver {@code AccountService}/{@code CreditCardService}/
     * {@code TransactionService}). Checagem deliberadamente estreita: confirma que a porta
     * <em>declara</em> acesso escopado, não que todo chamador o usa — ArchUnit não faz
     * taint-tracking de qual argumento chega em qual query, então isto não substitui revisão dos
     * call sites (ver {@code UserGuards}, que lê exclusivamente pelos finders escopados).
     */
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
                        continue; // TEMPORÁRIO fase 2→4 — ver javadoc da regra 6 e de isDissolvedContextRemnant
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
                        continue; // TEMPORÁRIO fase 2→4 — ver javadoc da regra e de isDissolvedContextRemnant
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
                        continue; // TEMPORÁRIO fase 2→4 — ver javadoc da regra e de isDissolvedContextRemnant
                    }
                    events.add(SimpleConditionEvent.violated(dependency,
                            "f%03d depende de f%03d (fatia irmã): %s"
                                    .formatted(from, to, dependency.getDescription())));
                }
            }
        };
    }

}
