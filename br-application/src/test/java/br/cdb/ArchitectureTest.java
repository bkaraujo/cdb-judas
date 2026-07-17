package br.cdb;

import br.commons.annotation.Facade;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.jspecify.annotations.NullMarked;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
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
                    .should().accessClassesThat().haveSimpleNameEndingWith("Repository")
                    .because("LoginResource é a exceção deliberada: autenticação acessa UserRepository direto (equivalente ao UserDetailsService do Spring), sem Facade de contexto para isso");

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
                    .because("o contexto é livre de framework: DI via Registry, validação na borda (@Valid nos *Request)");

    @ArchTest
    static final ArchRule user_feature_slices_must_not_depend_on_each_other =
            SlicesRuleDefinition.slices().matching("..feature.user.(*)..")
                    .should().notDependOnEachOther()
                    .ignoreDependency(alwaysTrue(), resideInAnyPackage(
                            "..feature.user.stream..", "..feature.user.deletion.."))
                    .because("a composição entre fatias mora no UserUseCase; stream (transporte SSE) e deletion (vocabulário) são compartilhados");

    @ArchTest
    static final ArchRule system_features_must_not_access_user_features =
            noClasses().that().resideInAPackage("..feature.system..")
                    .should().accessClassesThat().resideInAPackage("..feature.user..")
                    .because("features de sistema (auth, seed, catálogo) são a base; user.* depende delas, nunca o contrário");

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
