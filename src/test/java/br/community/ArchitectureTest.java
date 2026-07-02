package br.community;

import br.commons.annotation.Facade;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jspecify.annotations.NullMarked;

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
                    .because("feature deve acessar context exclusivamente via Facade, modelos de domínio (_0_domain.model) ou eventos de domínio (_0_domain.event)");

    @ArchTest
    static final ArchRule context_must_not_depend_on_spring =
            noClasses().that().resideInAPackage("..context..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("o contexto é livre de framework: a injeção é via Registry e o Spring fica na borda (feature/core)");

    @ArchTest
    static final ArchRule no_class_depends_on_spring =
            noClasses().that().resideInAPackage("br..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("a migração para Quarkus removeu o Spring da borda (feature/core); nenhuma classe do app deve depender dele");

    private static DescribedPredicate<JavaClass> contextClassNotExposedViaFacade() {
        return resideInAPackage("..context..")
                .and(not(resideInAPackage("..context..shared..")))
                .and(not(resideInAPackage("..context.._0_domain.model..")))
                .and(not(resideInAPackage("..context.._0_domain.event..")))
                .and(not(resideInAPackage("..context.._1_application.command..")))
                .and(not(resideInAPackage("..context.._1_application.event..")))
                .and(not(implement(Facade.class)));
    }

}
