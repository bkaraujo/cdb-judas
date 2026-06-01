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
                    .should().accessClassesThat().haveSimpleNameEndingWith("Repository");

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
                    .should().accessClassesThat().resideInAPackage("..feature..");

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
